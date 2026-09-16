package com.lc.kbagent.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpressionCalculatorTest {

    @Test
    void 基础四则运算与优先级() {
        assertEquals("7", ExpressionCalculator.eval("1 + 2 * 3"));
        assertEquals("9", ExpressionCalculator.eval("(1+2)*3"));
        assertEquals("25", ExpressionCalculator.eval("100/4"));
        assertEquals("5", ExpressionCalculator.eval("10-3-2"));
    }

    @Test
    void 小数与负数() {
        assertEquals("20", ExpressionCalculator.eval("2.5*8"));
        assertEquals("2", ExpressionCalculator.eval("-3+5"));
        assertEquals("2.5", ExpressionCalculator.eval("10/4"));
    }

    @Test
    void 非法表达式被拦截() {
        assertFalse(ExpressionCalculator.isValidExpression("select * from t"));
        assertFalse(ExpressionCalculator.isValidExpression(null));
        assertFalse(ExpressionCalculator.isValidExpression(""));
    }

    @Test
    void 除以零抛出异常() {
        try {
            ExpressionCalculator.eval("1/0");
            org.junit.jupiter.api.Assertions.fail("应当抛出异常");
        } catch (ArithmeticException e) {
            assertTrue(e.getMessage().contains("0"));
        }
    }
}
