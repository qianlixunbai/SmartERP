package com.smartoa.service;

import com.smartoa.common.BusinessException;
import com.smartoa.entity.JournalEntry;
import com.smartoa.mapper.JournalEntryMapper;
import com.smartoa.mapper.AccountMapper;
import com.smartoa.mapper.AuditLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AccountingServiceTest {

    @Autowired
    private AccountingService accountingService;

    @Autowired
    private JournalEntryMapper journalEntryMapper;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private AuditLogMapper auditLogMapper;

    // ========== 入账测试 ==========

    @Nested
    @DisplayName("入账（post）")
    class PostTests {

        @Test
        @DisplayName("正常入账 - 应生成借贷两条分录")
        void testPost_ShouldCreateTwoEntries() {
            String txnId = accountingService.post(
                    1L, "办公", new BigDecimal("500.00"), 1L, "测试入账", null, null);

            assertNotNull(txnId);

            List<JournalEntry> entries = journalEntryMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<JournalEntry>()
                            .eq(JournalEntry::getTransactionId, txnId));

            assertEquals(2, entries.size(), "应生成两条分录");

            BigDecimal totalDebit = entries.stream()
                    .map(JournalEntry::getDebit)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCredit = entries.stream()
                    .map(JournalEntry::getCredit)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertEquals(0, totalDebit.compareTo(totalCredit), "借贷必须平衡");
            assertEquals(0, new BigDecimal("500.00").compareTo(totalDebit), "借方应等于入账金额");
        }

        @Test
        @DisplayName("金额精度 - 应保留两位小数")
        void testPost_ShouldScaleToTwoDecimal() {
            String txnId = accountingService.post(
                    1L, "差旅", new BigDecimal("1234.567"), 1L, "精度测试", null, null);

            List<JournalEntry> entries = journalEntryMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<JournalEntry>()
                            .eq(JournalEntry::getTransactionId, txnId));

            for (JournalEntry e : entries) {
                if (e.getDebit().compareTo(BigDecimal.ZERO) > 0) {
                    assertEquals(0, new BigDecimal("1234.57").compareTo(e.getDebit()),
                            "金额应四舍五入到两位小数");
                }
            }
        }

        @Test
        @DisplayName("零金额入账 - 应抛出异常")
        void testPost_ZeroAmount_ShouldThrow() {
            assertThrows(BusinessException.class, () ->
                    accountingService.post(1L, "办公", BigDecimal.ZERO, 1L, "零金额", null, null));
        }

        @Test
        @DisplayName("负金额入账 - 应抛出异常")
        void testPost_NegativeAmount_ShouldThrow() {
            assertThrows(BusinessException.class, () ->
                    accountingService.post(1L, "办公", new BigDecimal("-100"), 1L, "负金额", null, null));
        }

        @Test
        @DisplayName("不同费用类别 - 应使用不同科目")
        void testPost_DifferentCategories_ShouldUseDifferentAccounts() {
            String txn1 = accountingService.post(1L, "办公", new BigDecimal("100"), 1L, null, null, null);
            String txn2 = accountingService.post(1L, "差旅", new BigDecimal("200"), 1L, null, null, null);

            List<JournalEntry> entries1 = journalEntryMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<JournalEntry>()
                            .eq(JournalEntry::getTransactionId, txn1));
            List<JournalEntry> entries2 = journalEntryMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<JournalEntry>()
                            .eq(JournalEntry::getTransactionId, txn2));

            // 借方科目ID应该不同（办公费 vs 差旅费）
            Long debitAcct1 = entries1.stream()
                    .filter(e -> e.getDebit().compareTo(BigDecimal.ZERO) > 0)
                    .findFirst().get().getAccountId();
            Long debitAcct2 = entries2.stream()
                    .filter(e -> e.getDebit().compareTo(BigDecimal.ZERO) > 0)
                    .findFirst().get().getAccountId();

            assertNotEquals(debitAcct1, debitAcct2, "不同类别应使用不同费用科目");
        }
    }

    // ========== 冲销测试 ==========

    @Nested
    @DisplayName("冲销（reverse）")
    class ReverseTests {

        @Test
        @DisplayName("正常冲销 - 应生成借贷互换的反向分录")
        void testReverse_ShouldCreateReversalEntries() {
            String originalTxn = accountingService.post(
                    1L, "办公", new BigDecimal("300.00"), 1L, "原始入账", null, null);

            String reversalTxn = accountingService.reverse(
                    originalTxn, 1L, "测试冲销");

            assertNotNull(reversalTxn);
            assertNotEquals(originalTxn, reversalTxn, "冲销应生成新的transactionId");

            List<JournalEntry> reversalEntries = journalEntryMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<JournalEntry>()
                            .eq(JournalEntry::getTransactionId, reversalTxn));

            assertEquals(2, reversalEntries.size());

            // 验证借贷互换
            List<JournalEntry> originals = journalEntryMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<JournalEntry>()
                            .eq(JournalEntry::getTransactionId, originalTxn));

            BigDecimal origDebit = originals.stream()
                    .map(JournalEntry::getDebit)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal reversalCredit = reversalEntries.stream()
                    .map(JournalEntry::getCredit)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertEquals(0, origDebit.compareTo(reversalCredit),
                    "冲销贷方应等于原始借方");
        }

        @Test
        @DisplayName("重复冲销 - 应抛出异常")
        void testReverse_AlreadyReversed_ShouldThrow() {
            String txnId = accountingService.post(
                    1L, "办公", new BigDecimal("100"), 1L, null, null, null);
            accountingService.reverse(txnId, 1L, "第一次冲销");

            assertThrows(BusinessException.class, () ->
                    accountingService.reverse(txnId, 1L, "第二次冲销"));
        }

        @Test
        @DisplayName("冲销不存在的分录 - 应抛出异常")
        void testReverse_NotFound_ShouldThrow() {
            assertThrows(BusinessException.class, () ->
                    accountingService.reverse("non-existent-uuid", 1L, "不存在"));
        }
    }

    // ========== 试算平衡测试 ==========

    @Nested
    @DisplayName("试算平衡")
    class TrialBalanceTests {

        @Test
        @DisplayName("多笔入账后试算应平衡")
        void testTrialBalance_ShouldBeBalanced() {
            Map<String, Object> before = accountingService.trialBalance();
            BigDecimal prevDebit = (BigDecimal) before.get("totalDebit");

            accountingService.post(1L, "办公", new BigDecimal("100"), 1L, null, null, null);
            accountingService.post(1L, "差旅", new BigDecimal("200"), 1L, null, null, null);
            accountingService.post(1L, "招待", new BigDecimal("300"), 1L, null, null, null);

            Map<String, Object> result = accountingService.trialBalance();

            assertEquals(true, result.get("balanced"));
            BigDecimal totalDebit = (BigDecimal) result.get("totalDebit");
            BigDecimal totalCredit = (BigDecimal) result.get("totalCredit");
            assertEquals(0, totalDebit.compareTo(totalCredit));
            // 验证新增 600 的借方金额
            assertEquals(0, new BigDecimal("600.00").compareTo(totalDebit.subtract(prevDebit)));
        }

        @Test
        @DisplayName("入账+冲销后试算应平衡")
        void testTrialBalance_AfterReversal_ShouldBeBalanced() {
            String txnId = accountingService.post(
                    1L, "办公", new BigDecimal("500"), 1L, null, null, null);
            accountingService.reverse(txnId, 1L, "冲销");

            Map<String, Object> result = accountingService.trialBalance();

            assertEquals(true, result.get("balanced"),
                    "入账+冲销后净额为0，试算应平衡");
        }
    }

    // ========== 科目余额测试 ==========

    @Nested
    @DisplayName("科目余额")
    class BalanceTests {

        @Test
        @DisplayName("入账后科目余额应正确计算")
        void testAccountBalances_ShouldCalculateCorrectly() {
            accountingService.post(1L, "办公", new BigDecimal("200"), 1L, null, null, null);

            List<Map<String, Object>> balances = accountingService.accountBalances();

            assertFalse(balances.isEmpty());

            // 找办公费科目（6001）
            Map<String, Object> officeAccount = balances.stream()
                    .filter(b -> "6001".equals(b.get("code")))
                    .findFirst().orElse(null);

            assertNotNull(officeAccount);
            BigDecimal balance = (BigDecimal) officeAccount.get("balance");
            assertEquals(0, new BigDecimal("200.00").compareTo(balance),
                    "办公费科目借方余额应为200");
        }
    }
}
