package com.lc.kbagent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * 工具：安全四则运算计算器。
 *
 * <p>自己实现了递归下降解析器（仅支持数字与 + - * / ( )），
 * 不使用 ScriptEngine / eval，杜绝表达式注入等安全问题。
 */
@Component
public class CalculatorTool implements Tool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern EXPR_PATTERN = Pattern.compile("^[0-9+*/().\\s-]+$");
    private static final int MAX_LENGTH = 100;

    @Override
    public String name() {
        return "calculate";
    }

    @Override
    public String description() {
        return "执行数学四则运算，支持括号与小数。当用户需要精确计算结果时使用，例如 (1+2)*3、100/4、2.5*8。参数 expression 为运算表达式。";
    }

    @Override
    public JsonNode parametersSchema() {
        ObjectNode params = JsonNodeFactory.instance.objectNode();
        params.put("type", "object");
        ObjectNode properties = JsonNodeFactory.instance.objectNode();
        ObjectNode expr = JsonNodeFactory.instance.objectNode();
        expr.put("type", "string");
        expr.put("description", "数学表达式，仅支持数字与 + - * / ( )，如 (1+2)*3");
        properties.set("expression", expr);
        params.set("properties", properties);
        params.set("required", JsonNodeFactory.instance.arrayNode().add("expression"));
        return params;
    }

    @Override
    public String execute(String argumentsJson) {
        String expression = parseExpression(argumentsJson);
        if (expression == null || expression.isBlank()) {
            return "参数解析失败：请提供 expression 参数，例如 {\"expression\": \"(1+2)*3\"}";
        }
        if (expression.length() > MAX_LENGTH || !EXPR_PATTERN.matcher(expression).matches()) {
            return "表达式不合法：仅支持数字与 + - * / ( ) 运算，例如 (1+2)*3";
        }
        try {
            double result = new Parser(expression).parse();
            if (Double.isNaN(result) || Double.isInfinite(result)) {
                return "计算结果无效（可能存在除以 0 的情况）";
            }
            return "计算结果：" + expression + " = " + format(result);
        } catch (Exception e) {
            return "表达式无法解析，请检查语法是否正确，例如 (1+2)*3";
        }
    }

    private String parseExpression(String argumentsJson) {
        try {
            JsonNode node = MAPPER.readTree(argumentsJson);
            JsonNode expr = node.path("expression");
            return expr.isTextual() ? expr.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String format(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        BigDecimal decimal = BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros();
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
