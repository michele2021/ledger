#!/usr/bin/env python3
import argparse
import csv
import json
import re
from pathlib import Path


EXPR_OVERRIDES = {
    "[D_6]": 'arrivalCountry == "DE" && warehouseCountry == "DE"',
    "[D_OSS_2]": 'arrivalCountry == "DE"',
    "[D_OSS_35]": 'isSpecialReducedRate && dateBefore(taxDate, "2021-01-01")',
    "[D_OSS_351]": 'isSpecialReducedRate && dateBefore(taxDate, "2021-01-01")',
    "[D_OSS_36]": 'isSpecialReducedRate && dateBefore(taxDate, "2021-01-01")',
    "[D_4]": '!isArrivalInEu || isArrivingInSpecialTerritoryEX',
    "[D_10]": '!isArrivalInEu || isArrivingInSpecialTerritoryEX',
    "[D_14]": 'isWarehouseInEu && !isEuImport',
    "[D_OSS_5]": '!isArrivalInEu || isArrivingInSpecialTerritoryEX',
    "[D_OSS_27]": 'isWarehouseInEu && (isArrivingInSpecialTerritoryEX || !isArrivalInEu)',
    "[D_OSS_28]": 'isWarehouseInEu && warehouseCountry != "GB" && isArrivalInEu && !isArrivingInSpecialTerritoryEX && isBorderCrossingB2B',
    "[D_OSS_38]": 'isClientRegisteredForOSS(period)',
    "[D_OSS_39]": 'isRefund && warehouseCountry == "DE" && arrivalCountry == "GB" && isBeforeBrexit(taxDate)',
    "[D_OSS_41]": 'isRefund && warehouseCountry == "GB" && isBeforeBrexit(taxDate)',
    "[D_OSS_42]": 'isRefund && arrivalCountry == "GB" && isBeforeBrexit(taxDate)',
    "[D_20]": 'isRefund && taxCountry == "GB" && isBeforeBrexit(saleTaxDate)',
}

OUTCOME_OVERRIDES = {
    ("[D_6]", "false"): "notDomesticInDeNode",
    ("[D_OSS_2]", "false"): "deliveryFromDeToNonDe",
    ("[D_7]", "false"): "domesticInDeNonExNode",
    ("[D_OSS_3]", "false"): "vatRateNode",
    ("[D_OSS_39]", "false"): "impliedNextNode",
}

STOP_WORDS = {
    "terminal",
    "grouped",
    "go",
    "to",
    "posting",
    "key",
    "net",
    "vat",
    "and",
    "implied",
    "next",
    "node",
    "in",
    "graph",
}


def normalize_expression(rule_id, expression):
    expression = expression.strip()
    if rule_id in EXPR_OVERRIDES:
        return EXPR_OVERRIDES[rule_id]
    expression = expression.replace(" AND ", " && ").replace(" OR ", " || ")
    expression = re.sub(r"\b([A-Za-z0-9_]+)\s*==\s*True\b", r"\1", expression, flags=re.IGNORECASE)
    expression = re.sub(r"\b([A-Za-z0-9_]+)\s*==\s*False\b", r"!\1", expression, flags=re.IGNORECASE)
    expression = expression.replace("NOT ", "!")
    expression = expression.replace("  ", " ").strip()
    return expression


def normalize_outcome(rule_id, branch, value):
    if not value:
        return value
    override = OUTCOME_OVERRIDES.get((rule_id, branch))
    if override:
        return override

    value = value.strip()
    for prefix in ("Go to ", "Terminal: ", "Terminal :", "Grouped "):
        if value.startswith(prefix):
            value = value[len(prefix):].strip()
            break

    value = re.sub(r"\([^)]*\)", "", value).strip()
    value = value.replace("&", " ")

    tokens = re.findall(r"[A-Za-z0-9_]+", value)
    filtered = []
    for token in tokens:
        normalized_token = token.strip("_")
        if not normalized_token:
            continue
        if normalized_token.isdigit():
            continue
        if normalized_token.lower() in STOP_WORDS:
            continue
        filtered.append(normalized_token)

    if not filtered:
        return value
    if len(filtered) == 1:
        return filtered[0]
    return "_AND_".join(filtered)


def normalize_outcome_map(rule_id, raw):
    if not raw:
        return raw
    try:
        outcome_map = json.loads(raw)
    except json.JSONDecodeError:
        return raw
    normalized = {}
    for key, value in outcome_map.items():
        normalized[key] = normalize_outcome(rule_id, None, str(value))
    return json.dumps(normalized, ensure_ascii=True)


def main():
    parser = argparse.ArgumentParser(description="Normalize rule expressions/outcomes to DSL.")
    parser.add_argument("input_csv", type=Path, help="Input normalized CSV path")
    parser.add_argument("output_csv", type=Path, help="Output CSV path")
    args = parser.parse_args()

    rows = []
    with args.input_csv.open(newline="", encoding="utf-8") as infile:
        reader = csv.DictReader(infile)
        for row in reader:
            rule_id = row["rule_id"].strip()
            row["expression"] = normalize_expression(rule_id, row["expression"])
            row["true_outcome"] = normalize_outcome(rule_id, "true", row["true_outcome"])
            row["false_outcome"] = normalize_outcome(rule_id, "false", row["false_outcome"])
            if row["rule_type"].upper() in ("SWITCH", "AI"):
                row["outcome_map"] = normalize_outcome_map(rule_id, row.get("outcome_map", ""))
            rows.append(row)

    with args.output_csv.open("w", newline="", encoding="utf-8") as outfile:
        writer = csv.DictWriter(outfile, fieldnames=reader.fieldnames)
        writer.writeheader()
        for row in rows:
            writer.writerow(row)


if __name__ == "__main__":
    main()
