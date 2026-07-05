package com.smartoa.service;

import com.smartoa.common.BusinessException;
import com.smartoa.dto.DividendRequest;
import com.smartoa.dto.TradeRequest;
import com.smartoa.entity.PortfolioDividend;
import com.smartoa.entity.PortfolioTrade;
import com.smartoa.mapper.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PortfolioServiceTest {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private PortfolioTradeMapper tradeMapper;

    @Autowired
    private PortfolioDividendMapper dividendMapper;

    @Autowired
    private PortfolioHoldingMapper holdingMapper;

    // ========== 交易执行测试 ==========

    @Nested
    @DisplayName("交易执行（executeTrade）")
    class TradeTests {

        @Test
        @DisplayName("正常买入 - 应创建交易记录并更新持仓")
        void testBuy_ShouldCreateTradeAndHolding() {
            TradeRequest req = new TradeRequest();
            req.setPortfolioId(1L);
            req.setAssetId(1L); // QQQM
            req.setTradeType("BUY");
            req.setQuantity(new BigDecimal("5.000000"));
            req.setPrice(new BigDecimal("220.0000"));
            req.setFee(new BigDecimal("1.10"));
            req.setTradeDate(LocalDate.now());
            req.setMemo("测试买入");

            PortfolioTrade trade = portfolioService.executeTrade(req, 1L);

            assertNotNull(trade);
            assertNotNull(trade.getId());
            assertEquals("BUY", trade.getTradeType());
            assertEquals(0, new BigDecimal("1100.00").compareTo(trade.getTotalAmount()));
        }

        @Test
        @DisplayName("加仓买入 - 应更新加权平均成本")
        void testBuy_AddPosition_ShouldUpdateWeightedAvgCost() {
            // 种子数据 QQQM 15股 @ $200 = $3000
            // 再买 5股 @ $220 = $1100
            TradeRequest req = new TradeRequest();
            req.setPortfolioId(1L);
            req.setAssetId(1L);
            req.setTradeType("BUY");
            req.setQuantity(new BigDecimal("5.000000"));
            req.setPrice(new BigDecimal("220.0000"));
            req.setFee(BigDecimal.ZERO);
            req.setTradeDate(LocalDate.now());

            portfolioService.executeTrade(req, 1L);

            // 20股，总成本 4100，均价 205
            List<Map<String, Object>> holdings = portfolioService.getHoldingsSummary(1L);
            Map<String, Object> qqmHolding = holdings.stream()
                    .filter(h -> "QQQM".equals(h.get("symbol")))
                    .findFirst().orElse(null);

            assertNotNull(qqmHolding);
            assertEquals(0, new BigDecimal("20.000000").compareTo((BigDecimal) qqmHolding.get("quantity")));
            assertEquals(0, new BigDecimal("205.0000").compareTo((BigDecimal) qqmHolding.get("avgCost")));
        }

        @Test
        @DisplayName("正常卖出 - 应减少持仓数量")
        void testSell_ShouldReduceQuantity() {
            TradeRequest req = new TradeRequest();
            req.setPortfolioId(1L);
            req.setAssetId(1L); // QQQM 15股
            req.setTradeType("SELL");
            req.setQuantity(new BigDecimal("5.000000"));
            req.setPrice(new BigDecimal("230.0000"));
            req.setFee(BigDecimal.ZERO);
            req.setTradeDate(LocalDate.now());

            PortfolioTrade trade = portfolioService.executeTrade(req, 1L);

            assertNotNull(trade);
            assertEquals("SELL", trade.getTradeType());

            // 检查持仓变为10股
            List<Map<String, Object>> holdings = portfolioService.getHoldingsSummary(1L);
            Map<String, Object> qqmHolding = holdings.stream()
                    .filter(h -> "QQQM".equals(h.get("symbol")))
                    .findFirst().orElse(null);

            assertNotNull(qqmHolding);
            assertEquals(0, new BigDecimal("10.000000").compareTo((BigDecimal) qqmHolding.get("quantity")));
        }

        @Test
        @DisplayName("卖出超出持仓 - 应抛出异常")
        void testSell_ExceedHolding_ShouldThrow() {
            TradeRequest req = new TradeRequest();
            req.setPortfolioId(1L);
            req.setAssetId(1L); // QQQM 15股
            req.setTradeType("SELL");
            req.setQuantity(new BigDecimal("99.000000"));
            req.setPrice(new BigDecimal("220.0000"));
            req.setFee(BigDecimal.ZERO);
            req.setTradeDate(LocalDate.now());

            assertThrows(BusinessException.class, () -> portfolioService.executeTrade(req, 1L));
        }

        @Test
        @DisplayName("不存在的组合 - 应抛出异常")
        void testTrade_NonexistentPortfolio_ShouldThrow() {
            TradeRequest req = new TradeRequest();
            req.setPortfolioId(999L);
            req.setAssetId(1L);
            req.setTradeType("BUY");
            req.setQuantity(new BigDecimal("1"));
            req.setPrice(new BigDecimal("100"));
            req.setFee(BigDecimal.ZERO);
            req.setTradeDate(LocalDate.now());

            assertThrows(BusinessException.class, () -> portfolioService.executeTrade(req, 1L));
        }
    }

    // ========== 股息测试 ==========

    @Nested
    @DisplayName("股息记录（recordDividend）")
    class DividendTests {

        @Test
        @DisplayName("正常记录股息 - 应创建股息记录")
        void testRecordDividend_ShouldCreate() {
            DividendRequest req = new DividendRequest();
            req.setPortfolioId(1L);
            req.setAssetId(1L);
            req.setAmount(new BigDecimal("15.00"));
            req.setPerShare(new BigDecimal("1.0000"));
            req.setQuantity(new BigDecimal("15.000000"));
            req.setDividendDate(LocalDate.now());

            PortfolioDividend dividend = portfolioService.recordDividend(req);

            assertNotNull(dividend);
            assertNotNull(dividend.getId());
            assertEquals(0, new BigDecimal("15.00").compareTo(dividend.getAmount()));
        }

        @Test
        @DisplayName("不存在的组合记录股息 - 应抛出异常")
        void testRecordDividend_NonexistentPortfolio_ShouldThrow() {
            DividendRequest req = new DividendRequest();
            req.setPortfolioId(999L);
            req.setAssetId(1L);
            req.setAmount(new BigDecimal("10"));
            req.setPerShare(new BigDecimal("1"));
            req.setQuantity(new BigDecimal("10"));
            req.setDividendDate(LocalDate.now());

            assertThrows(BusinessException.class, () -> portfolioService.recordDividend(req));
        }
    }

    // ========== 持仓汇总测试 ==========

    @Nested
    @DisplayName("持仓汇总（getHoldingsSummary）")
    class HoldingsTests {

        @Test
        @DisplayName("持仓汇总应包含正确的市值和盈亏")
        void testHoldingsSummary_ShouldCalculateCorrectly() {
            List<Map<String, Object>> holdings = portfolioService.getHoldingsSummary(1L);

            assertFalse(holdings.isEmpty(), "种子数据应有持仓");
            assertEquals(3, holdings.size(), "应有3条持仓");

            // 检查 QQQM
            Map<String, Object> qqm = holdings.stream()
                    .filter(h -> "QQQM".equals(h.get("symbol")))
                    .findFirst().orElse(null);

            assertNotNull(qqm);
            assertEquals(0, new BigDecimal("15.000000").compareTo((BigDecimal) qqm.get("quantity")));
            assertEquals(0, new BigDecimal("200.0000").compareTo((BigDecimal) qqm.get("avgCost")));
            // 市值 = 15 * $220 = $3300
            assertEquals(0, new BigDecimal("3300.00").compareTo((BigDecimal) qqm.get("marketValue")));
            // 盈亏 = 3300 - 3000 = 300
            assertEquals(0, new BigDecimal("300.00").compareTo((BigDecimal) qqm.get("unrealizedPnl")));
        }
    }

    // ========== 组合绩效测试 ==========

    @Nested
    @DisplayName("组合绩效（getPortfolioPerformance）")
    class PerformanceTests {

        @Test
        @DisplayName("绩效应返回正确指标")
        void testPerformance_ShouldReturnMetrics() {
            Map<String, Object> perf = portfolioService.getPortfolioPerformance(1L);

            assertNotNull(perf);
            assertTrue(perf.containsKey("totalCost"));
            assertTrue(perf.containsKey("totalMarketValue"));
            assertTrue(perf.containsKey("totalReturnPercent"));
            assertTrue(perf.containsKey("annualizedReturnPercent"));
            assertTrue(perf.containsKey("totalDividends"));

            // 总成本 = 3000 + 5000 + 5400 = 13400
            BigDecimal totalCost = (BigDecimal) perf.get("totalCost");
            assertEquals(0, new BigDecimal("13400.00").compareTo(totalCost));

            // 总市值 = 15*220 + 10*530 + 20*290 = 3300+5300+5800 = 14400
            BigDecimal totalMV = (BigDecimal) perf.get("totalMarketValue");
            assertEquals(0, new BigDecimal("14400.00").compareTo(totalMV));
        }
    }

    // ========== 资产配置测试 ==========

    @Nested
    @DisplayName("资产配置（getAssetAllocation）")
    class AllocationTests {

        @Test
        @DisplayName("配置百分比合计应为100%")
        void testAllocation_ShouldSumTo100() {
            List<Map<String, Object>> allocation = portfolioService.getAssetAllocation(1L);

            assertFalse(allocation.isEmpty());

            BigDecimal total = allocation.stream()
                    .map(a -> (BigDecimal) a.get("allocationPercent"))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(0, java.math.RoundingMode.HALF_UP);

            assertEquals(0, new BigDecimal("100").compareTo(total), "配置百分比合计应为100%");
        }
    }

    // ========== 看板测试 ==========

    @Nested
    @DisplayName("看板数据（getPortfolioDashboard）")
    class DashboardTests {

        @Test
        @DisplayName("看板应包含所有子模块数据")
        void testDashboard_ShouldContainAllSections() {
            Map<String, Object> dashboard = portfolioService.getPortfolioDashboard(1L);

            assertNotNull(dashboard);
            assertTrue(dashboard.containsKey("portfolio"));
            assertTrue(dashboard.containsKey("holdings"));
            assertTrue(dashboard.containsKey("totalMarketValue"));
            assertTrue(dashboard.containsKey("totalCost"));
            assertTrue(dashboard.containsKey("totalPnl"));
            assertTrue(dashboard.containsKey("performance"));
            assertTrue(dashboard.containsKey("maxDrawdown"));
            assertTrue(dashboard.containsKey("sharpeRatio"));
            assertTrue(dashboard.containsKey("allocation"));
            assertTrue(dashboard.containsKey("totalDividends"));
        }
    }
}
