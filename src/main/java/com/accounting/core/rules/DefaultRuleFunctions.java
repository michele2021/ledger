package com.accounting.core.rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DefaultRuleFunctions {
    private DefaultRuleFunctions() {
    }

    public static Map<String, RuleFunction> defaultFunctions() {
        Map<String, RuleFunction> functions = new HashMap<>();
        functions.put("date", DefaultRuleFunctions::date);
        functions.put("dateBefore", DefaultRuleFunctions::dateBefore);
        functions.put("dateOnOrBefore", DefaultRuleFunctions::dateOnOrBefore);
        functions.put("dateAfter", DefaultRuleFunctions::dateAfter);
        functions.put("dateOnOrAfter", DefaultRuleFunctions::dateOnOrAfter);
        functions.put("isIn", DefaultRuleFunctions::isIn);
        return functions;
    }

    private static Object date(List<Object> args, RuleEvaluationContext context) {
        if (args.size() != 1) {
            throw new RuleEngineException("date() requires exactly one argument.");
        }
        return toDate(args.getFirst());
    }

    private static Object dateBefore(List<Object> args, RuleEvaluationContext context) {
        if (args.size() != 2) {
            throw new RuleEngineException("dateBefore() requires two arguments.");
        }
        LocalDate left = toDate(args.get(0));
        LocalDate right = toDate(args.get(1));
        return left.isBefore(right);
    }

    private static Object dateOnOrBefore(List<Object> args, RuleEvaluationContext context) {
        if (args.size() != 2) {
            throw new RuleEngineException("dateOnOrBefore() requires two arguments.");
        }
        LocalDate left = toDate(args.get(0));
        LocalDate right = toDate(args.get(1));
        return left.isBefore(right) || left.isEqual(right);
    }

    private static Object dateAfter(List<Object> args, RuleEvaluationContext context) {
        if (args.size() != 2) {
            throw new RuleEngineException("dateAfter() requires two arguments.");
        }
        LocalDate left = toDate(args.get(0));
        LocalDate right = toDate(args.get(1));
        return left.isAfter(right);
    }

    private static Object dateOnOrAfter(List<Object> args, RuleEvaluationContext context) {
        if (args.size() != 2) {
            throw new RuleEngineException("dateOnOrAfter() requires two arguments.");
        }
        LocalDate left = toDate(args.get(0));
        LocalDate right = toDate(args.get(1));
        return left.isAfter(right) || left.isEqual(right);
    }

    private static Object isIn(List<Object> args, RuleEvaluationContext context) {
        if (args.size() < 2) {
            throw new RuleEngineException("isIn() requires at least two arguments.");
        }
        Object value = args.getFirst();
        for (int i = 1; i < args.size(); i++) {
            if (equalsValue(value, args.get(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean equalsValue(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (left instanceof Number && right instanceof Number) {
            return toBigDecimal((Number) left).compareTo(toBigDecimal((Number) right)) == 0;
        }
        return left.equals(right);
    }

    private static BigDecimal toBigDecimal(Number number) {
        return new BigDecimal(number.toString());
    }

    private static LocalDate toDate(Object value) {
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof String stringValue) {
            try {
                return LocalDate.parse(stringValue);
            } catch (DateTimeParseException e) {
                throw new RuleEngineException("Invalid date literal: " + stringValue, e);
            }
        }
        throw new RuleEngineException("Unsupported date argument: " + value);
    }
}
