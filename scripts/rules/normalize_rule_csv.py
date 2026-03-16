#!/usr/bin/env python3
import argparse
import csv
import json
import re
from pathlib import Path


def infer_rule_type(technical_condition):
    if technical_condition and "switch" in technical_condition.lower():
        return "SWITCH"
    return "BOOLEAN"


def infer_expression(technical_condition):
    if not technical_condition:
        return ""
    match = re.search(r"based on\s+([A-Za-z0-9_.]+)", technical_condition, re.IGNORECASE)
    if match:
        return match.group(1)
    return technical_condition.strip()


def normalize(input_path, output_path):
    def parse_outcome_map(text):
        if not text:
            return {}
        parts = re.split(r"[;\n]", text)
        items = []
        for part in parts:
            part = part.strip()
            if not part:
                continue
            if "->" in part:
                key, value = part.split("->", 1)
            elif ":" in part:
                key, value = part.split(":", 1)
            else:
                continue
            items.append((key.strip(), value.strip()))
        return dict(items)

    with open(input_path, newline="", encoding="utf-8") as infile:
        reader = csv.DictReader(infile)
        rows = []
        for row in reader:
            rule_id = (row.get("Rule ID") or row.get("rule_id") or "").strip()
            if not rule_id:
                continue
            technical = (row.get("Technical Condition") or row.get("technical_condition") or "").strip()
            rule_type = infer_rule_type(technical)
            expression = infer_expression(technical)
            true_outcome = (row.get("True Outcome") or row.get("true_outcome") or "").strip()
            false_outcome = (row.get("False Outcome") or row.get("false_outcome") or "").strip()
            outcome_map = {}
            if rule_type == "SWITCH":
                outcome_map = parse_outcome_map(true_outcome)
            rows.append({
                "rule_id": rule_id,
                "section": (row.get("Section") or row.get("section") or "").strip(),
                "description": (row.get("Business Description") or row.get("description") or "").strip(),
                "rule_type": rule_type,
                "expression": expression,
                "true_outcome": true_outcome if rule_type == "BOOLEAN" else "",
                "false_outcome": false_outcome if rule_type == "BOOLEAN" else "",
                "outcome_map": json.dumps(outcome_map, ensure_ascii=True) if outcome_map else ""
            })

    fieldnames = [
        "rule_id",
        "section",
        "description",
        "rule_type",
        "expression",
        "true_outcome",
        "false_outcome",
        "outcome_map"
    ]

    with open(output_path, "w", newline="", encoding="utf-8") as outfile:
        writer = csv.DictWriter(outfile, fieldnames=fieldnames)
        writer.writeheader()
        for row in rows:
            writer.writerow(row)


def main():
    parser = argparse.ArgumentParser(description="Normalize rule CSV into engine-friendly format.")
    parser.add_argument("input_csv", type=Path, help="Input CSV path")
    parser.add_argument("output_csv", type=Path, help="Output normalized CSV path")
    args = parser.parse_args()
    normalize(args.input_csv, args.output_csv)


if __name__ == "__main__":
    main()
