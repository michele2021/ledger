package com.accounting.core.rules.io;

import com.accounting.core.rules.RuleEngineException;

import java.util.ArrayList;
import java.util.List;

final class CsvUtil {
    private CsvUtil() {
    }

    static List<String> parseLine(String line) {
        List<String> result = new ArrayList<>();
        if (line == null) {
            return result;
        }
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '"';
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == quoteChar) {
                    if (i + 1 < line.length() && line.charAt(i + 1) == quoteChar) {
                        current.append(quoteChar);
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"' || c == '\'') {
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == ',') {
                    result.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        if (inQuotes) {
            throw new RuleEngineException("Unterminated CSV quoted field: " + line);
        }
        result.add(current.toString());
        return result;
    }
}
