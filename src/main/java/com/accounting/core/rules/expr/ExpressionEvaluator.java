package com.accounting.core.rules.expr;

import com.accounting.core.rules.RuleEngineException;
import com.accounting.core.rules.RuleEvaluationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpressionEvaluator {
    private final Map<String, Node> cache = new HashMap<>();

    public boolean evaluateBoolean(String expression, RuleEvaluationContext context) {
        Object value = evaluateValue(expression, context);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        throw new RuleEngineException("Expression did not evaluate to boolean: " + expression);
    }

    public Object evaluateValue(String expression, RuleEvaluationContext context) {
        if (expression == null || expression.isBlank()) {
            throw new RuleEngineException("Empty expression.");
        }
        Node node = cache.computeIfAbsent(expression, this::parse);
        return node.eval(context);
    }

    private Node parse(String expression) {
        Lexer lexer = new Lexer(expression);
        Parser parser = new Parser(lexer);
        Node node = parser.parseExpression();
        if (parser.peek().type != TokenType.EOF) {
            throw new RuleEngineException("Unexpected token: " + parser.peek().lexeme);
        }
        return node;
    }

    private interface Node {
        Object eval(RuleEvaluationContext context);
    }

    private static class LiteralNode implements Node {
        private final Object value;

        private LiteralNode(Object value) {
            this.value = value;
        }

        @Override
        public Object eval(RuleEvaluationContext context) {
            return value;
        }
    }

    private static class IdentifierNode implements Node {
        private final String name;

        private IdentifierNode(String name) {
            this.name = name;
        }

        @Override
        public Object eval(RuleEvaluationContext context) {
            return context.resolveFact(name);
        }
    }

    private static class FunctionNode implements Node {
        private final String name;
        private final List<Node> args;

        private FunctionNode(String name, List<Node> args) {
            this.name = name;
            this.args = args;
        }

        @Override
        public Object eval(RuleEvaluationContext context) {
            List<Object> values = new ArrayList<>();
            for (Node arg : args) {
                values.add(arg.eval(context));
            }
            return context.invokeFunction(name, values);
        }
    }

    private static class UnaryNode implements Node {
        private final TokenType op;
        private final Node right;

        private UnaryNode(TokenType op, Node right) {
            this.op = op;
            this.right = right;
        }

        @Override
        public Object eval(RuleEvaluationContext context) {
            Object value = right.eval(context);
            if (op == TokenType.NOT) {
                return !toBoolean(value);
            }
            throw new RuleEngineException("Unsupported unary operator: " + op);
        }
    }

    private static class BinaryNode implements Node {
        private final Node left;
        private final TokenType op;
        private final Node right;

        private BinaryNode(Node left, TokenType op, Node right) {
            this.left = left;
            this.op = op;
            this.right = right;
        }

        @Override
        public Object eval(RuleEvaluationContext context) {
            switch (op) {
                case AND -> {
                    return toBoolean(left.eval(context)) && toBoolean(right.eval(context));
                }
                case OR -> {
                    return toBoolean(left.eval(context)) || toBoolean(right.eval(context));
                }
                case EQ, NE, LT, LTE, GT, GTE -> {
                    Object leftValue = left.eval(context);
                    Object rightValue = right.eval(context);
                    return compare(leftValue, rightValue, op);
                }
                default -> throw new RuleEngineException("Unsupported binary operator: " + op);
            }
        }
    }

    private static boolean compare(Object left, Object right, TokenType op) {
        int cmp;
        if (left == null || right == null) {
            if (op == TokenType.EQ) {
                return left == right;
            }
            if (op == TokenType.NE) {
                return left != right;
            }
            throw new RuleEngineException("Cannot compare null values with operator: " + op);
        }
        if (left instanceof Number && right instanceof Number) {
            cmp = toBigDecimal((Number) left).compareTo(toBigDecimal((Number) right));
        } else if (left instanceof LocalDate || right instanceof LocalDate) {
            LocalDate leftDate = toDate(left);
            LocalDate rightDate = toDate(right);
            cmp = leftDate.compareTo(rightDate);
        } else {
            cmp = left.toString().compareTo(right.toString());
        }
        return switch (op) {
            case EQ -> cmp == 0;
            case NE -> cmp != 0;
            case LT -> cmp < 0;
            case LTE -> cmp <= 0;
            case GT -> cmp > 0;
            case GTE -> cmp >= 0;
            default -> throw new RuleEngineException("Unsupported comparator: " + op);
        };
    }

    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        throw new RuleEngineException("Expected boolean but found: " + value);
    }

    private static BigDecimal toBigDecimal(Number number) {
        return new BigDecimal(number.toString());
    }

    private static LocalDate toDate(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof String stringValue) {
            try {
                return LocalDate.parse(stringValue);
            } catch (DateTimeParseException e) {
                throw new RuleEngineException("Invalid date literal: " + stringValue, e);
            }
        }
        throw new RuleEngineException("Expected date but found: " + value);
    }

    private enum TokenType {
        IDENT,
        STRING,
        NUMBER,
        LPAREN,
        RPAREN,
        COMMA,
        EQ,
        NE,
        LT,
        LTE,
        GT,
        GTE,
        AND,
        OR,
        NOT,
        EOF
    }

    private record Token(TokenType type, String lexeme) {
    }

    private static class Lexer {
        private final String source;
        private int index;

        private Lexer(String source) {
            this.source = source;
        }

        private Token nextToken() {
            skipWhitespace();
            if (index >= source.length()) {
                return new Token(TokenType.EOF, "");
            }
            char c = source.charAt(index);
            if (c == '(') {
                index++;
                return new Token(TokenType.LPAREN, "(");
            }
            if (c == ')') {
                index++;
                return new Token(TokenType.RPAREN, ")");
            }
            if (c == ',') {
                index++;
                return new Token(TokenType.COMMA, ",");
            }
            if (c == '"' || c == '\'') {
                return stringToken();
            }
            if (c == '=' && peekNext() == '=') {
                index += 2;
                return new Token(TokenType.EQ, "==");
            }
            if (c == '!' && peekNext() == '=') {
                index += 2;
                return new Token(TokenType.NE, "!=");
            }
            if (c == '<') {
                if (peekNext() == '=') {
                    index += 2;
                    return new Token(TokenType.LTE, "<=");
                }
                index++;
                return new Token(TokenType.LT, "<");
            }
            if (c == '>') {
                if (peekNext() == '=') {
                    index += 2;
                    return new Token(TokenType.GTE, ">=");
                }
                index++;
                return new Token(TokenType.GT, ">");
            }
            if (c == '&' && peekNext() == '&') {
                index += 2;
                return new Token(TokenType.AND, "&&");
            }
            if (c == '|' && peekNext() == '|') {
                index += 2;
                return new Token(TokenType.OR, "||");
            }
            if (isDigit(c) || (c == '-' && isDigit(peekNext()))) {
                return numberToken();
            }
            if (isAlpha(c)) {
                return identifierToken();
            }
            if (c == '!') {
                index++;
                return new Token(TokenType.NOT, "!");
            }
            throw new RuleEngineException("Unexpected character: " + c);
        }

        private Token stringToken() {
            char quote = source.charAt(index);
            index++;
            StringBuilder sb = new StringBuilder();
            while (index < source.length()) {
                char c = source.charAt(index);
                if (c == quote) {
                    index++;
                    return new Token(TokenType.STRING, sb.toString());
                }
                if (c == '\\' && index + 1 < source.length()) {
                    index++;
                    sb.append(source.charAt(index));
                } else {
                    sb.append(c);
                }
                index++;
            }
            throw new RuleEngineException("Unterminated string literal.");
        }

        private Token numberToken() {
            int start = index;
            if (source.charAt(index) == '-') {
                index++;
            }
            while (index < source.length() && isDigit(source.charAt(index))) {
                index++;
            }
            if (index < source.length() && source.charAt(index) == '.') {
                index++;
                while (index < source.length() && isDigit(source.charAt(index))) {
                    index++;
                }
            }
            return new Token(TokenType.NUMBER, source.substring(start, index));
        }

        private Token identifierToken() {
            int start = index;
            while (index < source.length() && (isAlphaNumeric(source.charAt(index)) || source.charAt(index) == '.')) {
                index++;
            }
            String lexeme = source.substring(start, index);
            String upper = lexeme.toUpperCase();
            if ("AND".equals(upper)) {
                return new Token(TokenType.AND, lexeme);
            }
            if ("OR".equals(upper)) {
                return new Token(TokenType.OR, lexeme);
            }
            if ("NOT".equals(upper)) {
                return new Token(TokenType.NOT, lexeme);
            }
            return new Token(TokenType.IDENT, lexeme);
        }

        private void skipWhitespace() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }

        private char peekNext() {
            if (index + 1 >= source.length()) {
                return '\0';
            }
            return source.charAt(index + 1);
        }

        private boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        private boolean isAlpha(char c) {
            return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
        }

        private boolean isAlphaNumeric(char c) {
            return isAlpha(c) || isDigit(c);
        }
    }

    private static class Parser {
        private final Lexer lexer;
        private Token current;

        private Parser(Lexer lexer) {
            this.lexer = lexer;
            this.current = lexer.nextToken();
        }

        private Token peek() {
            return current;
        }

        private Token advance() {
            Token previous = current;
            current = lexer.nextToken();
            return previous;
        }

        private boolean match(TokenType type) {
            if (current.type == type) {
                advance();
                return true;
            }
            return false;
        }

        private Node parseExpression() {
            return parseOr();
        }

        private Node parseOr() {
            Node node = parseAnd();
            while (match(TokenType.OR)) {
                node = new BinaryNode(node, TokenType.OR, parseAnd());
            }
            return node;
        }

        private Node parseAnd() {
            Node node = parseUnary();
            while (match(TokenType.AND)) {
                node = new BinaryNode(node, TokenType.AND, parseUnary());
            }
            return node;
        }

        private Node parseUnary() {
            if (match(TokenType.NOT)) {
                return new UnaryNode(TokenType.NOT, parseUnary());
            }
            return parseComparison();
        }

        private Node parseComparison() {
            Node node = parsePrimary();
            if (match(TokenType.EQ)) {
                return new BinaryNode(node, TokenType.EQ, parsePrimary());
            }
            if (match(TokenType.NE)) {
                return new BinaryNode(node, TokenType.NE, parsePrimary());
            }
            if (match(TokenType.LT)) {
                return new BinaryNode(node, TokenType.LT, parsePrimary());
            }
            if (match(TokenType.LTE)) {
                return new BinaryNode(node, TokenType.LTE, parsePrimary());
            }
            if (match(TokenType.GT)) {
                return new BinaryNode(node, TokenType.GT, parsePrimary());
            }
            if (match(TokenType.GTE)) {
                return new BinaryNode(node, TokenType.GTE, parsePrimary());
            }
            return node;
        }

        private Node parsePrimary() {
            Token token = current;
            if (match(TokenType.NUMBER)) {
                return new LiteralNode(parseNumber(token.lexeme));
            }
            if (match(TokenType.STRING)) {
                return new LiteralNode(token.lexeme);
            }
            if (match(TokenType.IDENT)) {
                String ident = token.lexeme;
                String upper = ident.toUpperCase();
                if ("TRUE".equals(upper)) {
                    return new LiteralNode(true);
                }
                if ("FALSE".equals(upper)) {
                    return new LiteralNode(false);
                }
                if ("NULL".equals(upper)) {
                    return new LiteralNode(null);
                }
                if (match(TokenType.LPAREN)) {
                    List<Node> args = new ArrayList<>();
                    if (!match(TokenType.RPAREN)) {
                        do {
                            args.add(parseExpression());
                        } while (match(TokenType.COMMA));
                        if (!match(TokenType.RPAREN)) {
                            throw new RuleEngineException("Expected ')' after function arguments.");
                        }
                    }
                    return new FunctionNode(ident, args);
                }
                return new IdentifierNode(ident);
            }
            if (match(TokenType.LPAREN)) {
                Node node = parseExpression();
                if (!match(TokenType.RPAREN)) {
                    throw new RuleEngineException("Expected ')' after expression.");
                }
                return node;
            }
            throw new RuleEngineException("Unexpected token: " + token.lexeme);
        }

        private Number parseNumber(String lexeme) {
            try {
                if (lexeme.contains(".")) {
                    return Double.parseDouble(lexeme);
                }
                return Long.parseLong(lexeme);
            } catch (NumberFormatException e) {
                throw new RuleEngineException("Invalid number literal: " + lexeme, e);
            }
        }
    }
}
