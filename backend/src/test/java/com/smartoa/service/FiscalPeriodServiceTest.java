package com.smartoa.service;

import com.smartoa.common.BusinessException;
import com.smartoa.entity.FiscalPeriod;
import com.smartoa.entity.PeriodBalance;
import com.smartoa.mapper.FiscalPeriodMapper;
import com.smartoa.mapper.PeriodBalanceMapper;
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
class FiscalPeriodServiceTest {

    @Autowired
    private FiscalPeriodService fiscalPeriodService;

    @Autowired
    private AccountingService accountingService;

    @Autowired
    private FiscalPeriodMapper fiscalPeriodMapper;

    @Autowired
    private PeriodBalanceMapper periodBalanceMapper;

    // ========== 期间查询 ==========

    @Nested
    @DisplayName("期间查询")
    class PeriodQueryTests {

        @Test
        @DisplayName("获取期间列表 - 应包含种子数据")
        void testGetPeriods_ShouldReturnSeedData() {
            List<FiscalPeriod> periods = fiscalPeriodService.getPeriods();
            assertFalse(periods.isEmpty(), "种子数据应至少有7个期间");
            assertTrue(periods.size() >= 7);
        }

        @Test
        @DisplayName("获取当前期间 - 应返回OPEN状态")
        void testGetCurrentPeriod_ShouldBeOpen() {
            FiscalPeriod current = fiscalPeriodService.getCurrentPeriod();
            assertNotNull(current, "应存在OPEN期间");
            assertEquals("OPEN", current.getStatus());
            assertEquals(2026, current.getYear());
            assertEquals(7, current.getMonth());
        }
    }

    // ========== 月结测试 ==========

    @Nested
    @DisplayName("月结（closePeriod）")
    class ClosePeriodTests {

        @Test
        @DisplayName("月结 - 应生成余额快照并锁定期间")
        void testClosePeriod_ShouldGenerateSnapshotsAndLock() {
            // 先在7月期间入账
            accountingService.post(1L, "办公", new BigDecimal("500"), 1L, "月结测试", null, null);
            accountingService.post(1L, "差旅", new BigDecimal("300"), 1L, "月结测试", null, null);

            // 月结7月
            fiscalPeriodService.closePeriod(2026, 7, 1L);

            // 验证期间已关闭
            FiscalPeriod period = fiscalPeriodMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FiscalPeriod>()
                            .eq(FiscalPeriod::getYear, 2026)
                            .eq(FiscalPeriod::getMonth, 7));
            assertNotNull(period);
            assertEquals("CLOSED", period.getStatus());
            assertEquals(1L, period.getClosedBy());

            // 验证余额快照已生成
            List<PeriodBalance> balances = fiscalPeriodService.getPeriodBalances(period.getId());
            assertFalse(balances.isEmpty(), "月结后应生成余额快照");

            // 验证借贷平衡
            BigDecimal totalDebit = balances.stream()
                    .map(PeriodBalance::getDebitTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCredit = balances.stream()
                    .map(PeriodBalance::getCreditTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, totalDebit.compareTo(totalCredit), "快照借贷应平衡");
        }

        @Test
        @DisplayName("重复月结 - 应抛出异常")
        void testClosePeriod_AlreadyClosed_ShouldThrow() {
            // 6月已是CLOSED
            assertThrows(BusinessException.class, () ->
                    fiscalPeriodService.closePeriod(2026, 6, 1L));
        }

        @Test
        @DisplayName("月结不存在的期间 - 应抛出异常")
        void testClosePeriod_NotExist_ShouldThrow() {
            assertThrows(BusinessException.class, () ->
                    fiscalPeriodService.closePeriod(2025, 1, 1L));
        }
    }

    // ========== 反月结测试 ==========

    @Nested
    @DisplayName("反月结（reopenPeriod）")
    class ReopenPeriodTests {

        @Test
        @DisplayName("反月结 - 应删除快照并重新打开期间")
        void testReopenPeriod_ShouldDeleteSnapshotsAndOpen() {
            // 月结 → 反月结
            fiscalPeriodService.closePeriod(2026, 7, 1L);
            fiscalPeriodService.reopenPeriod(2026, 7, 1L);

            FiscalPeriod period = fiscalPeriodMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FiscalPeriod>()
                            .eq(FiscalPeriod::getYear, 2026)
                            .eq(FiscalPeriod::getMonth, 7));
            assertEquals("OPEN", period.getStatus());
            assertNull(period.getClosedBy());

            // 快照应已删除
            List<PeriodBalance> balances = fiscalPeriodService.getPeriodBalances(period.getId());
            assertTrue(balances.isEmpty(), "反月结后快照应删除");
        }

        @Test
        @DisplayName("反月结已打开的期间 - 应抛出异常")
        void testReopenPeriod_AlreadyOpen_ShouldThrow() {
            assertThrows(BusinessException.class, () ->
                    fiscalPeriodService.reopenPeriod(2026, 7, 1L));
        }
    }

    // ========== 期间校验测试 ==========

    @Nested
    @DisplayName("期间校验（checkPeriodOpen）")
    class CheckPeriodTests {

        @Test
        @DisplayName("OPEN期间校验 - 应通过")
        void testCheckPeriodOpen_OpenPeriod_ShouldPass() {
            assertDoesNotThrow(() ->
                    fiscalPeriodService.checkPeriodOpen(2026, 7));
        }

        @Test
        @DisplayName("CLOSED期间校验 - 应抛出异常")
        void testCheckPeriodOpen_ClosedPeriod_ShouldThrow() {
            assertThrows(BusinessException.class, () ->
                    fiscalPeriodService.checkPeriodOpen(2026, 6));
        }

        @Test
        @DisplayName("不存在期间校验 - 应抛出异常")
        void testCheckPeriodOpen_NotExist_ShouldThrow() {
            assertThrows(BusinessException.class, () ->
                    fiscalPeriodService.checkPeriodOpen(2025, 1));
        }
    }

    // ========== 看板数据测试 ==========

    @Nested
    @DisplayName("看板数据（dashboard）")
    class DashboardTests {

        @Test
        @DisplayName("看板数据 - 月结后应返回汇总数据")
        void testDashboard_AfterClose_ShouldReturnData() {
            accountingService.post(1L, "办公", new BigDecimal("100"), 1L, null, null, null);
            fiscalPeriodService.closePeriod(2026, 7, 1L);

            FiscalPeriod period = fiscalPeriodMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FiscalPeriod>()
                            .eq(FiscalPeriod::getYear, 2026)
                            .eq(FiscalPeriod::getMonth, 7));

            Map<String, Object> data = fiscalPeriodService.getDashboardData(period.getId(), null, null);
            assertNotNull(data);
            assertNotNull(data.get("totalDebit"));
            assertNotNull(data.get("totalCredit"));
            assertTrue((int) data.get("balanceCount") > 0);
        }
    }
}
