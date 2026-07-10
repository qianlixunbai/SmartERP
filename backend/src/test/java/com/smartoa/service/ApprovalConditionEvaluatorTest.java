package com.smartoa.service;

import com.smartoa.common.BusinessException;
import com.smartoa.entity.ExpenseRequest;
import com.smartoa.entity.LeaveRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ApprovalConditionEvaluatorTest {

    private final ApprovalConditionEvaluator evaluator = new ApprovalConditionEvaluator();

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t\n"})
    void blankExpression_ShouldAllow(String expression) {
        assertTrue(evaluator.evaluateLeave(expression, leaveRequest()));
    }

    @Test
    void nullExpression_ShouldAllow() {
        assertTrue(evaluator.evaluateExpense(null, expenseRequest()));
    }

    @ParameterizedTest
    @MethodSource("leaveExpressions")
    void legalLeaveExpressions_ShouldEvaluate(String expression, boolean expected) {
        assertEquals(expected, evaluator.evaluateLeave(expression, leaveRequest()));
    }

    private static Stream<Arguments> leaveExpressions() {
        return Stream.of(
                Arguments.of("days <= 5", true),
                Arguments.of("days > 5", false),
                Arguments.of("days > 3 && leaveType == '年假'", true),
                Arguments.of("(days > 3 && leaveType == '年假') || leaveType == '病假'", true),
                Arguments.of("startDate < endDate", true),
                Arguments.of("days > 3 and not (leaveType == '病假')", true)
        );
    }

    @ParameterizedTest
    @MethodSource("expenseExpressions")
    void legalExpenseExpressions_ShouldEvaluate(String expression, boolean expected) {
        assertEquals(expected, evaluator.evaluateExpense(expression, expenseRequest()));
    }

    private static Stream<Arguments> expenseExpressions() {
        return Stream.of(
                Arguments.of("amount > 1000", true),
                Arguments.of("amount >= 1000 && category == '差旅'", true),
                Arguments.of("amount > 1000.49", true),
                Arguments.of("amount > 2000", false)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"salary > 1000", "applicant.role == 'MANAGER'"})
    void unknownVariable_ShouldFailClosed(String expression) {
        assertInvalid(() -> evaluator.evaluateLeave(expression, leaveRequest()));
    }

    @ParameterizedTest
    @MethodSource("dangerousExpressions")
    void dangerousSyntax_ShouldFailClosed(String expression, boolean expenseExpression) {
        if (expenseExpression) {
            assertInvalid(() -> evaluator.evaluateExpense(expression, expenseRequest()));
        } else {
            assertInvalid(() -> evaluator.evaluateLeave(expression, leaveRequest()));
        }
    }

    private static Stream<Arguments> dangerousExpressions() {
        return Stream.of(
                Arguments.of("T(java.lang.Runtime).getRuntime()", false),
                Arguments.of("new java.io.File('x')", false),
                Arguments.of("@environment", false),
                Arguments.of("#root", false),
                Arguments.of("getClass()", false),
                Arguments.of("amount.toString()", true),
                Arguments.of("days = 1", false)
        );
    }

    @ParameterizedTest
    @MethodSource("nonBooleanExpressions")
    void nonBooleanResult_ShouldFailClosed(String expression, boolean expenseExpression) {
        if (expenseExpression) {
            assertInvalid(() -> evaluator.evaluateExpense(expression, expenseRequest()));
        } else {
            assertInvalid(() -> evaluator.evaluateLeave(expression, leaveRequest()));
        }
    }

    private static Stream<Arguments> nonBooleanExpressions() {
        return Stream.of(
                Arguments.of("'true'", false),
                Arguments.of("days", false),
                Arguments.of("null", false),
                Arguments.of("amount + 1", true)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"days >", "amount === 1"})
    void syntaxError_ShouldFailClosed(String expression) {
        assertInvalid(() -> evaluator.evaluateLeave(expression, leaveRequest()));
    }

    @Test
    void expressionLongerThanDatabaseColumn_ShouldFailClosed() {
        assertInvalid(() -> evaluator.evaluateLeave("x".repeat(ApprovalConditionEvaluator.MAX_EXPRESSION_LENGTH + 1),
                leaveRequest()));
    }

    private void assertInvalid(org.junit.jupiter.api.function.Executable executable) {
        BusinessException exception = assertThrows(BusinessException.class, executable);
        assertEquals(422, exception.getCode());
        assertEquals("审批条件配置无效，请联系管理员", exception.getMessage());
    }

    private LeaveRequest leaveRequest() {
        LeaveRequest request = new LeaveRequest();
        request.setLeaveType("年假");
        request.setStartDate(LocalDate.of(2026, 1, 1));
        request.setEndDate(LocalDate.of(2026, 1, 5));
        return request;
    }

    private ExpenseRequest expenseRequest() {
        ExpenseRequest request = new ExpenseRequest();
        request.setAmount(new BigDecimal("1000.50"));
        request.setCategory("差旅");
        return request;
    }
}
