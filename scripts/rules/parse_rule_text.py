#!/usr/bin/env python3
import argparse
import csv
from pathlib import Path


def parse_blocks(lines):
    blocks = []
    current = []
    for line in lines:
        stripped = line.strip()
        if not stripped:
            continue
        if stripped.startswith("[") and stripped.endswith("]") or stripped.startswith("[") and " / " in stripped:
            if current:
                blocks.append(current)
            current = [stripped]
        else:
            current.append(stripped)
    if current:
        blocks.append(current)
    return blocks


def blocks_to_rows(blocks):
    rows = []
    for block in blocks:
        if len(block) < 4:
            continue
        rule_id_line = block[0]
        section = block[1] if len(block) > 1 else ""
        description = block[2] if len(block) > 2 else ""
        technical = block[3] if len(block) > 3 else ""
        true_outcome = block[4] if len(block) > 4 else ""
        false_outcome = block[5] if len(block) > 5 else ""
        if len(block) > 6:
            extra = " ".join(block[6:])
            if false_outcome:
                false_outcome = f"{false_outcome} {extra}"
            elif true_outcome:
                true_outcome = f"{true_outcome} {extra}"
            else:
                technical = f"{technical} {extra}"

        rule_ids = [rid.strip() for rid in rule_id_line.split(" / ")]
        for rule_id in rule_ids:
            rows.append({
                "Rule ID": rule_id,
                "Section": section,
                "Business Description": description,
                "Technical Condition": technical,
                "True Outcome": true_outcome,
                "False Outcome": false_outcome
            })
    return rows


def main():
    parser = argparse.ArgumentParser(description="Parse rule text blocks into CSV.")
    parser.add_argument("input_txt", type=Path, help="Input text file")
    parser.add_argument("output_csv", type=Path, help="Output CSV file")
    args = parser.parse_args()

    lines = args.input_txt.read_text(encoding="utf-8").splitlines()
    blocks = parse_blocks(lines)
    rows = blocks_to_rows(blocks)

    fieldnames = [
        "Rule ID",
        "Section",
        "Business Description",
        "Technical Condition",
        "True Outcome",
        "False Outcome"
    ]

    with args.output_csv.open("w", newline="", encoding="utf-8") as outfile:
        writer = csv.DictWriter(outfile, fieldnames=fieldnames)
        writer.writeheader()
        for row in rows:
            writer.writerow(row)


if __name__ == "__main__":
    main()
