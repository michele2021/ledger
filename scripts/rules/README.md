# Rule CSV Normalization

This script converts the human-friendly rule CSV into a normalized format that the rule engine can load.

Usage:
```bash
python scripts/rules/normalize_rule_csv.py input_rules.csv rules_normalized.csv
```

Notes:
- The script infers `rule_type` based on the presence of the word "switch" in `Technical Condition`.
- Switch outcomes are parsed from the `True Outcome` column using `KEY -> VALUE` or `KEY : VALUE` tokens.
- For non-boolean or more complex conditions (e.g. date phrases), update the normalized CSV manually.
- Always verify switch outcome parsing; adjust the `outcome_map` if any value is truncated or missing.

Optional helper:
```bash
python scripts/rules/parse_rule_text.py raw_rules.txt rules.csv
```
