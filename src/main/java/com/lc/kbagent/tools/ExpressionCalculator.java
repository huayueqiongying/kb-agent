package com.lc.kbagent.tools;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * 安全四则运算解析器（递归下降实现）。
 *
 * <p>不使用 ScriptEngine / eval，杜绝表达式注入等安全问题；
 * 仅支持数字与 + - * / ( ) 。
 */
public final class ExpressionCalculator {

    private static final Pattern EXPR_PATTERN = Pattern.compile("^[0-9+*/().\\s-]+$");
    private static final int MAX_LENGTH = 100;

    private ExpressionCalculator() {
    }

    /** 校验表达式是否可安全计算 */
    public static boolean isValidExpression(String expression) {
        return expression != null
                && !expression.isBlank()
                && expression.length() <= MAX_LENGTH
                && EXPR_PATTERN.matcher(expression).matches();
    }

    /**
     * 计算表达式并格式化为字符串。
     *
     * @throws IllegalArgumentException 语法错误
     * @throws ArithmeticException      除以 0 / 结果无效
     */
    public static String eval(String expression) {
        double result = new Parser(expression).parse();
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            throw new ArithmeticException("结果无效（可能存在除以 0）");
        }
        return format(result);
    }

    private static String format(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        BigDecimal decimal = BigDecimal.valueOf(value)
                .setScale(6, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        return decimal.toPlainString();
    }

    /**
     * 递归下降表达式解析器：
     * <pre>
     * expression := term (('+'|'-') term)*
     * term       := factor (('*'|'/') factor)*
     * factor     := number | '(' expression ')' | ('-'|'+') factor
     * </pre>
     */
    static class Parser {

        private final String input;
        private int pos = 0;

        Parser(String input) {
            this.input = input;
        }

        double parse() {
            double value = expression();
            skipWhitespace();
            if (pos < input.length()) {
                throw new IllegalArgumentException("存在无法解析的字符");
            }
            return value;
        }

        private double expression() {
            double value = term();
            while (true) {
                skipWhitespace();
                if (pos >= input.length()) {
                    return value;
                }
                char op = input.charAt(pos);
                if (op == '+' || op == '-') {
                    pos++;
                    double right = term();
                    value = op == '+' ? value + right : value - right;
                } else {
                    return value;
                }
            }
        }

        private double term() {
            double value = factor();
            while (true) {
                skipWhitespace();
                if (pos >= input.length()) {
                    return value;
                }
                char op = input.charAt(pos);
                if (op == '*' || op == '/') {
                    pos++;
                    double right = factor();
                    if (op == '/') {
                        if (right == 0) {
                            throw new ArithmeticException("除以 0");
                        }
                        value /= right;
                    } else {
                        value *= right;
                    }
                } else {
                    return value;
                }
            }
        }

        private double factor() {
            skipWhitespace();
            if (pos >= input.length()) {
                throw new IllegalArgumentException("表达式不完整");
            }
            char c = input.charAt(pos);
            if (c == '(') {
                pos++;
                double value = expression();
                skipWhitespace();
                if (pos >= input.length() || input.charAt(pos) != ')') {
                    throw new IllegalArgumentException("缺少右括号");
                }
                pos++;
                return value;
            }
            if (c == '-') {
                pos++;
                return -factor();
            }
            if (c == '+') {
                pos++;
                return factor();
            }
            return number();
        }

        private double number() {
            int start = pos;
            while (pos < input.length()
                    && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
                pos++;
            }
            if (start == pos) {
                throw new IllegalArgumentException("非法字符");
            }
            return Double.parseDouble(input.substring(start, pos));
        }

        private void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }
    }
}
