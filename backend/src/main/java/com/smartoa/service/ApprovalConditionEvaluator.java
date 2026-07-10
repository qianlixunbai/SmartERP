package com.smartoa.service;

import com.smartoa.common.BusinessException;
import com.smartoa.entity.ExpenseRequest;
import com.smartoa.entity.LeaveRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 受限审批条件执行器。
 * <p>
 * 不使用 SpEL：仅解析显式允许的字面量、比较、布尔运算和变量，
 * 防止条件配置访问对象方法、类型、Bean 或其他运行时能力。
 */
@Slf4j
@Component
public class ApprovalConditionEvaluator {

    /** 与 approval_node.condition_expression 的 VARCHAR(500) 定义保持一致。 */
    public static final int MAX_EXPRESSION_LENGTH = 500;
    private static final String INVALID_CONDITION_MESSAGE = "审批条件配置无效，请联系管理员";

    public boolean evaluateLeave(String expression, LeaveRequest request) {
        if (isBlank(expression)) {
            return true;
        }

        try {
            LeaveConditionContext context = new LeaveConditionContext(
                    ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1,
                    request.getLeaveType(), request.getStartDate(), request.getEndDate());
            Map<String, Object> variables = new HashMap<>();
            variables.put("days", context.days());
            variables.put("leaveType", context.leaveType());
            variables.put("startDate", context.startDate());
            variables.put("endDate", context.endDate());
            return evaluate(expression, variables);
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw invalidCondition(expression, e);
        }
    }

    public boolean evaluateExpense(String expression, ExpenseRequest request) {
        if (isBlank(expression)) {
            return true;
        }

        try {
            ExpenseConditionContext context = new ExpenseConditionContext(request.getAmount(), request.getCategory());
            Map<String, Object> variables = new HashMap<>();
            variables.put("amount", context.amount());
            variables.put("category", context.category());
            return evaluate(expression, variables);
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw invalidCondition(expression, e);
        }
    }

    private boolean evaluate(String expression, Map<String, Object> variables) {
        if (expression.length() > MAX_EXPRESSION_LENGTH) {
            throw invalidCondition(expression, new ConditionParseException("expression too long"));
        }

        try {
            Object result = new Parser(expression, variables).parseExpression();
            if (!(result instanceof Boolean booleanResult)) {
                throw new ConditionParseException("result is not boolean");
            }
            return booleanResult;
        } catch (RuntimeException e) {
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw invalidCondition(expression, e);
        }
    }

    private BusinessException invalidCondition(String expression, RuntimeException cause) {
        log.warn("审批条件配置无效: expr='{}', reason={}", expression, cause.getClass().getSimpleName());
        return new BusinessException(422, INVALID_CONDITION_MESSAGE);
    }

    private static boolean isBlank(String expression) {
        return expression == null || expression.isBlank();
    }

    private record LeaveConditionContext(long days, String leaveType, LocalDate startDate, LocalDate endDate) {
    }

    private record ExpenseConditionContext(BigDecimal amount, String category) {
    }

    private static final class Parser {
        private final Lexer lexer;
        private final Map<String, Object> variables;
        private Token current;

        private Parser(String expression, Map<String, Object> variables) {
            this.lexer = new Lexer(expression);
            this.variables = variables;
            this.current = lexer.next();
        }

        private Object parseExpression() {
            Object result = parseOr();
            expect(TokenType.END);
            return result;
        }

        private Object parseOr() {
            Object result = parseAnd();
            while (match(TokenType.OR)) {
                Object right = parseAnd();
                result = requireBoolean(result) || requireBoolean(right);
            }
            return result;
        }

        private Object parseAnd() {
            Object result = parseNot();
            while (match(TokenType.AND)) {
                Object right = parseNot();
                result = requireBoolean(result) && requireBoolean(right);
            }
            return result;
        }

        private Object parseNot() {
            if (match(TokenType.NOT)) {
                return !requireBoolean(parseNot());
            }
            return parseComparison();
        }

        private Object parseComparison() {
            Object left = parsePrimary();
            if (!current.type().isComparison()) {
                return left;
            }

            TokenType operator = current.type();
            advance();
            Object right = parsePrimary();
            return compare(left, right, operator);
        }

        private Object parsePrimary() {
            Token token = current;
            return switch (token.type()) {
                case LEFT_PAREN -> {
                    advance();
                    Object result = parseOr();
                    expect(TokenType.RIGHT_PAREN);
                    yield result;
                }
                case STRING -> {
                    advance();
                    yield token.text();
                }
                case NUMBER -> {
                    advance();
                    yield new BigDecimal(token.text());
                }
                case TRUE -> {
                    advance();
                    yield true;
                }
                case FALSE -> {
                    advance();
                    yield false;
                }
                case NULL -> {
                    advance();
                    yield null;
                }
                case IDENTIFIER -> {
                    advance();
                    if (!variables.containsKey(token.text())) {
                        throw new ConditionParseException("unknown variable");
                    }
                    yield variables.get(token.text());
                }
                default -> throw new ConditionParseException("unexpected token");
            };
        }

        private boolean compare(Object left, Object right, TokenType operator) {
            if (operator == TokenType.EQUAL || operator == TokenType.NOT_EQUAL) {
                boolean equal = valuesEqual(left, right);
                return operator == TokenType.EQUAL ? equal : !equal;
            }

            int comparison = compareOrdered(left, right);
            return switch (operator) {
                case GREATER_THAN -> comparison > 0;
                case GREATER_THAN_OR_EQUAL -> comparison >= 0;
                case LESS_THAN -> comparison < 0;
                case LESS_THAN_OR_EQUAL -> comparison <= 0;
                default -> throw new ConditionParseException("unexpected comparison");
            };
        }

        private boolean valuesEqual(Object left, Object right) {
            if (left instanceof Number && right instanceof Number) {
                return asBigDecimal(left).compareTo(asBigDecimal(right)) == 0;
            }
            return Objects.equals(left, right);
        }

        private int compareOrdered(Object left, Object right) {
            if (left instanceof Number && right instanceof Number) {
                return asBigDecimal(left).compareTo(asBigDecimal(right));
            }
            if (left instanceof LocalDate leftDate && right instanceof LocalDate rightDate) {
                return leftDate.compareTo(rightDate);
            }
            throw new ConditionParseException("incompatible comparison");
        }

        private BigDecimal asBigDecimal(Object value) {
            if (value instanceof BigDecimal decimal) {
                return decimal;
            }
            if (value instanceof Number number) {
                return new BigDecimal(number.toString());
            }
            throw new ConditionParseException("not a number");
        }

        private boolean requireBoolean(Object value) {
            if (value instanceof Boolean booleanValue) {
                return booleanValue;
            }
            throw new ConditionParseException("boolean required");
        }

        private boolean match(TokenType type) {
            if (current.type() != type) {
                return false;
            }
            advance();
            return true;
        }

        private void expect(TokenType type) {
            if (current.type() != type) {
                throw new ConditionParseException("unexpected token");
            }
            advance();
        }

        private void advance() {
            current = lexer.next();
        }
    }

    private static final class Lexer {
        private final String expression;
        private int index;

        private Lexer(String expression) {
            this.expression = expression;
        }

        private Token next() {
            skipWhitespace();
            if (index >= expression.length()) {
                return new Token(TokenType.END, "", index);
            }

            char character = expression.charAt(index);
            if (character == '\'' || character == '"') {
                return readString(character);
            }
            if (Character.isDigit(character) || (character == '-' && hasNextDigit())) {
                return readNumber();
            }
            if (Character.isLetter(character) || character == '_') {
                return readIdentifier();
            }

            int position = index++;
            return switch (character) {
                case '(' -> new Token(TokenType.LEFT_PAREN, "(", position);
                case ')' -> new Token(TokenType.RIGHT_PAREN, ")", position);
                case '!' -> match('=')
                        ? new Token(TokenType.NOT_EQUAL, "!=", position)
                        : new Token(TokenType.NOT, "!", position);
                case '=' -> {
                    if (match('=')) {
                        yield new Token(TokenType.EQUAL, "==", position);
                    }
                    throw new ConditionParseException("assignment is not allowed");
                }
                case '>' -> match('=')
                        ? new Token(TokenType.GREATER_THAN_OR_EQUAL, ">=", position)
                        : new Token(TokenType.GREATER_THAN, ">", position);
                case '<' -> match('=')
                        ? new Token(TokenType.LESS_THAN_OR_EQUAL, "<=", position)
                        : new Token(TokenType.LESS_THAN, "<", position);
                case '&' -> {
                    if (match('&')) {
                        yield new Token(TokenType.AND, "&&", position);
                    }
                    throw new ConditionParseException("unexpected character");
                }
                case '|' -> {
                    if (match('|')) {
                        yield new Token(TokenType.OR, "||", position);
                    }
                    throw new ConditionParseException("unexpected character");
                }
                default -> throw new ConditionParseException("unexpected character");
            };
        }

        private Token readString(char quote) {
            int position = index++;
            StringBuilder value = new StringBuilder();
            while (index < expression.length()) {
                char character = expression.charAt(index++);
                if (character == quote) {
                    return new Token(TokenType.STRING, value.toString(), position);
                }
                if (character == '\\') {
                    if (index >= expression.length()) {
                        throw new ConditionParseException("unterminated string");
                    }
                    value.append(expression.charAt(index++));
                } else {
                    value.append(character);
                }
            }
            throw new ConditionParseException("unterminated string");
        }

        private Token readNumber() {
            int position = index;
            if (expression.charAt(index) == '-') {
                index++;
            }
            while (index < expression.length() && Character.isDigit(expression.charAt(index))) {
                index++;
            }
            if (index < expression.length() && expression.charAt(index) == '.') {
                index++;
                int fractionStart = index;
                while (index < expression.length() && Character.isDigit(expression.charAt(index))) {
                    index++;
                }
                if (fractionStart == index) {
                    throw new ConditionParseException("invalid number");
                }
            }
            return new Token(TokenType.NUMBER, expression.substring(position, index), position);
        }

        private Token readIdentifier() {
            int position = index++;
            while (index < expression.length()) {
                char character = expression.charAt(index);
                if (!Character.isLetterOrDigit(character) && character != '_') {
                    break;
                }
                index++;
            }
            String value = expression.substring(position, index);
            return switch (value) {
                case "true" -> new Token(TokenType.TRUE, value, position);
                case "false" -> new Token(TokenType.FALSE, value, position);
                case "null" -> new Token(TokenType.NULL, value, position);
                case "and" -> new Token(TokenType.AND, value, position);
                case "or" -> new Token(TokenType.OR, value, position);
                case "not" -> new Token(TokenType.NOT, value, position);
                default -> new Token(TokenType.IDENTIFIER, value, position);
            };
        }

        private void skipWhitespace() {
            while (index < expression.length() && Character.isWhitespace(expression.charAt(index))) {
                index++;
            }
        }

        private boolean match(char expected) {
            if (index >= expression.length() || expression.charAt(index) != expected) {
                return false;
            }
            index++;
            return true;
        }

        private boolean hasNextDigit() {
            return index + 1 < expression.length() && Character.isDigit(expression.charAt(index + 1));
        }
    }

    private enum TokenType {
        END,
        IDENTIFIER,
        STRING,
        NUMBER,
        TRUE,
        FALSE,
        NULL,
        LEFT_PAREN,
        RIGHT_PAREN,
        EQUAL,
        NOT_EQUAL,
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL,
        LESS_THAN,
        LESS_THAN_OR_EQUAL,
        AND,
        OR,
        NOT;

        private boolean isComparison() {
            return this == EQUAL || this == NOT_EQUAL
                    || this == GREATER_THAN || this == GREATER_THAN_OR_EQUAL
                    || this == LESS_THAN || this == LESS_THAN_OR_EQUAL;
        }
    }

    private record Token(TokenType type, String text, int position) {
    }

    private static final class ConditionParseException extends RuntimeException {
        private ConditionParseException(String message) {
            super(message);
        }
    }
}
