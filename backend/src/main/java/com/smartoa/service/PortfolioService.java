package com.smartoa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartoa.common.BusinessException;
import com.smartoa.dto.DividendRequest;
import com.smartoa.dto.TradeRequest;
import com.smartoa.entity.*;
import com.smartoa.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PortfolioService {

    private static final BigDecimal RISK_FREE_RATE = new BigDecimal("0.03");
    private static final int TRADING_DAYS_PER_YEAR = 252;

    private final PortfolioMapper portfolioMapper;
    private final PortfolioAssetMapper assetMapper;
    private final PortfolioHoldingMapper holdingMapper;
    private final PortfolioTradeMapper tradeMapper;
    private final PortfolioDividendMapper dividendMapper;

    public PortfolioService(PortfolioMapper portfolioMapper,
                            PortfolioAssetMapper assetMapper,
                            PortfolioHoldingMapper holdingMapper,
                            PortfolioTradeMapper tradeMapper,
                            PortfolioDividendMapper dividendMapper) {
        this.portfolioMapper = portfolioMapper;
        this.assetMapper = assetMapper;
        this.holdingMapper = holdingMapper;
        this.tradeMapper = tradeMapper;
        this.dividendMapper = dividendMapper;
    }

    // ==================== 组合 CRUD ====================

    public List<Portfolio> getPortfolios() {
        return portfolioMapper.selectList(
                new LambdaQueryWrapper<Portfolio>().eq(Portfolio::getActive, true));
    }

    public Portfolio createPortfolio(String name, String description, String currency) {
        Portfolio p = new Portfolio();
        p.setName(name);
        p.setDescription(description);
        p.setBaseCurrency(currency != null ? currency : "USD");
        p.setActive(true);
        p.setCreateTime(LocalDateTime.now());
        p.setUpdateTime(LocalDateTime.now());
        portfolioMapper.insert(p);
        return p;
    }

    // ==================== 资产标的 CRUD ====================

    public List<PortfolioAsset> getAssets() {
        return assetMapper.selectList(null);
    }

    public PortfolioAsset createAsset(String symbol, String name, String assetType, String currency) {
        PortfolioAsset a = new PortfolioAsset();
        a.setSymbol(symbol.toUpperCase());
        a.setName(name);
        a.setAssetType(assetType);
        a.setCurrency(currency != null ? currency : "USD");
        a.setCreateTime(LocalDateTime.now());
        assetMapper.insert(a);
        return a;
    }

    public void updateAssetPrice(Long assetId, BigDecimal price) {
        PortfolioAsset asset = assetMapper.selectById(assetId);
        if (asset == null) {
            throw new BusinessException("资产标的不存在");
        }
        asset.setCurrentPrice(price.setScale(4, RoundingMode.HALF_UP));
        asset.setPriceDate(LocalDate.now());
        assetMapper.updateById(asset);
    }

    // ==================== 交易执行 ====================

    @Transactional
    public PortfolioTrade executeTrade(TradeRequest req, Long operatorId) {
        // 校验组合
        Portfolio portfolio = portfolioMapper.selectById(req.getPortfolioId());
        if (portfolio == null) {
            throw new BusinessException("投资组合不存在");
        }

        // 校验资产
        PortfolioAsset asset = assetMapper.selectById(req.getAssetId());
        if (asset == null) {
            throw new BusinessException("资产标的不存在");
        }

        BigDecimal quantity = req.getQuantity().setScale(6, RoundingMode.HALF_UP);
        BigDecimal price = req.getPrice().setScale(4, RoundingMode.HALF_UP);
        BigDecimal fee = req.getFee() != null ? req.getFee().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        BigDecimal totalAmount = quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);

        // SELL 校验持仓数量
        if ("SELL".equals(req.getTradeType())) {
            PortfolioHolding holding = getHolding(req.getPortfolioId(), req.getAssetId());
            if (holding == null || holding.getQuantity().compareTo(quantity) < 0) {
                throw new BusinessException("持仓不足，无法卖出");
            }
        }

        // 插入交易记录
        PortfolioTrade trade = new PortfolioTrade();
        trade.setPortfolioId(req.getPortfolioId());
        trade.setAssetId(req.getAssetId());
        trade.setTradeType(req.getTradeType());
        trade.setQuantity(quantity);
        trade.setPrice(price);
        trade.setTotalAmount(totalAmount);
        trade.setFee(fee);
        trade.setTradeDate(req.getTradeDate() != null ? req.getTradeDate() : LocalDate.now());
        trade.setMemo(req.getMemo());
        trade.setCreateTime(LocalDateTime.now());
        tradeMapper.insert(trade);

        // 更新持仓
        updateHolding(req.getPortfolioId(), req.getAssetId(), req.getTradeType(), quantity, price, totalAmount);

        log.info("交易执行: {} {} {} @ {} = {} (fee={})",
                req.getTradeType(), asset.getSymbol(), quantity, price, totalAmount, fee);
        return trade;
    }

    private PortfolioHolding getHolding(Long portfolioId, Long assetId) {
        return holdingMapper.selectOne(
                new LambdaQueryWrapper<PortfolioHolding>()
                        .eq(PortfolioHolding::getPortfolioId, portfolioId)
                        .eq(PortfolioHolding::getAssetId, assetId));
    }

    private void updateHolding(Long portfolioId, Long assetId,
                               String tradeType, BigDecimal quantity,
                               BigDecimal price, BigDecimal totalAmount) {
        PortfolioHolding holding = getHolding(portfolioId, assetId);
        LocalDateTime now = LocalDateTime.now();

        if ("BUY".equals(tradeType)) {
            if (holding == null) {
                // 新建持仓
                holding = new PortfolioHolding();
                holding.setPortfolioId(portfolioId);
                holding.setAssetId(assetId);
                holding.setQuantity(quantity);
                holding.setAvgCost(price);
                holding.setTotalCost(totalAmount);
                holding.setCreateTime(now);
                holding.setUpdateTime(now);
                holdingMapper.insert(holding);
            } else {
                // 加仓 — 加权平均成本
                BigDecimal oldQty = holding.getQuantity();
                BigDecimal oldCost = holding.getTotalCost();
                BigDecimal newQty = oldQty.add(quantity);
                BigDecimal newCost = oldCost.add(totalAmount);
                BigDecimal newAvg = newCost.divide(newQty, 4, RoundingMode.HALF_UP);

                holding.setQuantity(newQty);
                holding.setAvgCost(newAvg);
                holding.setTotalCost(newCost);
                holding.setUpdateTime(now);
                holdingMapper.updateById(holding);
            }
        } else { // SELL
            BigDecimal newQty = holding.getQuantity().subtract(quantity);
            if (newQty.compareTo(BigDecimal.ZERO) <= 0) {
                // 清仓
                holdingMapper.deleteById(holding.getId());
            } else {
                BigDecimal newCost = holding.getAvgCost().multiply(newQty).setScale(2, RoundingMode.HALF_UP);
                holding.setQuantity(newQty);
                holding.setTotalCost(newCost);
                holding.setUpdateTime(now);
                holdingMapper.updateById(holding);
            }
        }
    }

    // ==================== 股息记录 ====================

    public PortfolioDividend recordDividend(DividendRequest req) {
        Portfolio portfolio = portfolioMapper.selectById(req.getPortfolioId());
        if (portfolio == null) {
            throw new BusinessException("投资组合不存在");
        }
        PortfolioAsset asset = assetMapper.selectById(req.getAssetId());
        if (asset == null) {
            throw new BusinessException("资产标的不存在");
        }

        PortfolioDividend dividend = new PortfolioDividend();
        dividend.setPortfolioId(req.getPortfolioId());
        dividend.setAssetId(req.getAssetId());
        dividend.setAmount(req.getAmount().setScale(2, RoundingMode.HALF_UP));
        dividend.setPerShare(req.getPerShare().setScale(4, RoundingMode.HALF_UP));
        dividend.setQuantity(req.getQuantity().setScale(6, RoundingMode.HALF_UP));
        dividend.setDividendDate(req.getDividendDate());
        dividend.setCreateTime(LocalDateTime.now());
        dividendMapper.insert(dividend);

        log.info("股息记录: {} amount={} perShare={} qty={}",
                asset.getSymbol(), dividend.getAmount(), dividend.getPerShare(), dividend.getQuantity());
        return dividend;
    }

    // ==================== 持仓汇总 ====================

    public List<Map<String, Object>> getHoldingsSummary(Long portfolioId) {
        List<PortfolioHolding> holdings = holdingMapper.selectList(
                new LambdaQueryWrapper<PortfolioHolding>()
                        .eq(PortfolioHolding::getPortfolioId, portfolioId));
        if (holdings.isEmpty()) return Collections.emptyList();

        // 查所有资产价格
        Map<Long, PortfolioAsset> assetMap = assetMapper.selectList(null).stream()
                .collect(Collectors.toMap(PortfolioAsset::getId, a -> a));

        // 总市值
        BigDecimal totalMarketValue = BigDecimal.ZERO;
        List<Map<String, Object>> rows = new ArrayList<>();

        for (PortfolioHolding h : holdings) {
            PortfolioAsset asset = assetMap.get(h.getAssetId());
            if (asset == null) continue;

            BigDecimal currentPrice = asset.getCurrentPrice() != null
                    ? asset.getCurrentPrice() : BigDecimal.ZERO;
            BigDecimal marketValue = h.getQuantity().multiply(currentPrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal unrealizedPnl = marketValue.subtract(h.getTotalCost()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal pnlPct = h.getTotalCost().compareTo(BigDecimal.ZERO) > 0
                    ? unrealizedPnl.multiply(new BigDecimal("100"))
                    .divide(h.getTotalCost(), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("assetId", h.getAssetId());
            row.put("symbol", asset.getSymbol());
            row.put("assetName", asset.getName());
            row.put("assetType", asset.getAssetType());
            row.put("quantity", h.getQuantity());
            row.put("avgCost", h.getAvgCost());
            row.put("totalCost", h.getTotalCost());
            row.put("currentPrice", currentPrice);
            row.put("marketValue", marketValue);
            row.put("unrealizedPnl", unrealizedPnl);
            row.put("pnlPercent", pnlPct);
            rows.add(row);

            totalMarketValue = totalMarketValue.add(marketValue);
        }

        // 计算占比
        BigDecimal finalTotal = totalMarketValue;
        rows.forEach(row -> {
            BigDecimal mv = (BigDecimal) row.get("marketValue");
            BigDecimal alloc = finalTotal.compareTo(BigDecimal.ZERO) > 0
                    ? mv.multiply(new BigDecimal("100"))
                    .divide(finalTotal, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            row.put("allocation", alloc);
        });

        return rows;
    }

    // ==================== 组合绩效 ====================

    public Map<String, Object> getPortfolioPerformance(Long portfolioId) {
        List<PortfolioHolding> holdings = holdingMapper.selectList(
                new LambdaQueryWrapper<PortfolioHolding>()
                        .eq(PortfolioHolding::getPortfolioId, portfolioId));

        Map<Long, PortfolioAsset> assetMap = assetMapper.selectList(null).stream()
                .collect(Collectors.toMap(PortfolioAsset::getId, a -> a));

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalMarketValue = BigDecimal.ZERO;

        for (PortfolioHolding h : holdings) {
            PortfolioAsset asset = assetMap.get(h.getAssetId());
            if (asset == null) continue;
            BigDecimal currentPrice = asset.getCurrentPrice() != null ? asset.getCurrentPrice() : BigDecimal.ZERO;
            totalCost = totalCost.add(h.getTotalCost());
            totalMarketValue = totalMarketValue.add(h.getQuantity().multiply(currentPrice));
        }

        // 股息收入
        BigDecimal totalDividends = dividendMapper.selectList(
                new LambdaQueryWrapper<PortfolioDividend>()
                        .eq(PortfolioDividend::getPortfolioId, portfolioId))
                .stream()
                .map(PortfolioDividend::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // 总收益率 = (市值 + 股息 - 成本) / 成本
        BigDecimal totalReturn = BigDecimal.ZERO;
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            totalReturn = totalMarketValue.add(totalDividends).subtract(totalCost)
                    .multiply(new BigDecimal("100"))
                    .divide(totalCost, 2, RoundingMode.HALF_UP);
        }

        // 年化收益率（基于最早交易到现在的天数）
        BigDecimal annualizedReturn = BigDecimal.ZERO;
        List<PortfolioTrade> trades = tradeMapper.selectList(
                new LambdaQueryWrapper<PortfolioTrade>()
                        .eq(PortfolioTrade::getPortfolioId, portfolioId)
                        .orderByAsc(PortfolioTrade::getTradeDate));
        if (!trades.isEmpty()) {
            long days = ChronoUnit.DAYS.between(trades.get(0).getTradeDate(), LocalDate.now());
            if (days > 0) {
                double totalReturnDecimal = totalReturn.doubleValue() / 100.0;
                double years = days / 365.0;
                if (totalReturnDecimal > -1.0 && years > 0) {
                    double annualized = Math.pow(1.0 + totalReturnDecimal, 1.0 / years) - 1.0;
                    annualizedReturn = BigDecimal.valueOf(annualized * 100).setScale(2, RoundingMode.HALF_UP);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCost", totalCost.setScale(2, RoundingMode.HALF_UP));
        result.put("totalMarketValue", totalMarketValue.setScale(2, RoundingMode.HALF_UP));
        result.put("totalDividends", totalDividends);
        result.put("totalReturnPercent", totalReturn);
        result.put("annualizedReturnPercent", annualizedReturn);
        result.put("holdingCount", holdings.size());
        return result;
    }

    // ==================== 最大回撤 ====================

    public Map<String, Object> getMaxDrawdown(Long portfolioId) {
        // 基于交易历史推算每日投入成本曲线
        List<PortfolioTrade> trades = tradeMapper.selectList(
                new LambdaQueryWrapper<PortfolioTrade>()
                        .eq(PortfolioTrade::getPortfolioId, portfolioId)
                        .orderByAsc(PortfolioTrade::getTradeDate));

        Map<Long, PortfolioAsset> assetMap = assetMapper.selectList(null).stream()
                .collect(Collectors.toMap(PortfolioAsset::getId, a -> a));

        if (trades.isEmpty()) {
            return Map.of("maxDrawdownPercent", BigDecimal.ZERO,
                    "peakValue", BigDecimal.ZERO,
                    "troughValue", BigDecimal.ZERO);
        }

        // 按日期累加投入成本，计算当前市值对应的最大回撤
        BigDecimal cumulativeCost = BigDecimal.ZERO;
        BigDecimal peak = BigDecimal.ZERO;
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        for (PortfolioTrade t : trades) {
            if ("BUY".equals(t.getTradeType())) {
                cumulativeCost = cumulativeCost.add(t.getTotalAmount());
            } else {
                cumulativeCost = cumulativeCost.subtract(t.getTotalAmount());
            }
            if (cumulativeCost.compareTo(peak) > 0) {
                peak = cumulativeCost;
            }
            BigDecimal drawdown = peak.compareTo(BigDecimal.ZERO) > 0
                    ? peak.subtract(cumulativeCost).multiply(new BigDecimal("100"))
                    .divide(peak, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }

        // 当前市值 vs 成本的回撤
        BigDecimal currentMarketValue = BigDecimal.ZERO;
        List<PortfolioHolding> holdings = holdingMapper.selectList(
                new LambdaQueryWrapper<PortfolioHolding>()
                        .eq(PortfolioHolding::getPortfolioId, portfolioId));
        for (PortfolioHolding h : holdings) {
            PortfolioAsset asset = assetMap.get(h.getAssetId());
            if (asset == null || asset.getCurrentPrice() == null) continue;
            currentMarketValue = currentMarketValue.add(h.getQuantity().multiply(asset.getCurrentPrice()));
        }

        BigDecimal totalCost = holdings.stream()
                .map(PortfolioHolding::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentDD = totalCost.subtract(currentMarketValue)
                    .multiply(new BigDecimal("100"))
                    .divide(totalCost, 2, RoundingMode.HALF_UP);
            if (currentDD.compareTo(maxDrawdown) > 0) {
                maxDrawdown = currentDD;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("maxDrawdownPercent", maxDrawdown.max(BigDecimal.ZERO));
        result.put("peakValue", peak.setScale(2, RoundingMode.HALF_UP));
        result.put("troughValue", cumulativeCost.setScale(2, RoundingMode.HALF_UP));
        return result;
    }

    // ==================== 夏普比率 ====================

    public Map<String, Object> getSharpeRatio(Long portfolioId) {
        Map<String, Object> perf = getPortfolioPerformance(portfolioId);
        BigDecimal totalReturnPct = (BigDecimal) perf.get("totalReturnPercent");
        BigDecimal annualizedReturnPct = (BigDecimal) perf.get("annualizedReturnPercent");

        Map<String, Object> volatility = getVolatility(portfolioId);
        BigDecimal vol = (BigDecimal) volatility.get("annualizedVolatility");

        // Sharpe = (年化收益 - 无风险利率) / 波动率
        BigDecimal sharpe = BigDecimal.ZERO;
        if (vol.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal excessReturn = annualizedReturnPct.subtract(RISK_FREE_RATE.multiply(new BigDecimal("100")));
            sharpe = excessReturn.divide(vol, 4, RoundingMode.HALF_UP);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sharpeRatio", sharpe);
        result.put("annualizedReturn", annualizedReturnPct);
        result.put("riskFreeRate", RISK_FREE_RATE.multiply(new BigDecimal("100")));
        result.put("annualizedVolatility", vol);
        return result;
    }

    // ==================== 波动率 ====================

    public Map<String, Object> getVolatility(Long portfolioId) {
        // 基于各资产价格和持仓权重估算组合波动率
        // 简化模型：用资产价格的年化标准差 × 权重加权
        List<PortfolioHolding> holdings = holdingMapper.selectList(
                new LambdaQueryWrapper<PortfolioHolding>()
                        .eq(PortfolioHolding::getPortfolioId, portfolioId));

        Map<Long, PortfolioAsset> assetMap = assetMapper.selectList(null).stream()
                .collect(Collectors.toMap(PortfolioAsset::getId, a -> a));

        BigDecimal totalMarketValue = BigDecimal.ZERO;
        for (PortfolioHolding h : holdings) {
            PortfolioAsset asset = assetMap.get(h.getAssetId());
            if (asset == null || asset.getCurrentPrice() == null) continue;
            totalMarketValue = totalMarketValue.add(h.getQuantity().multiply(asset.getCurrentPrice()));
        }

        // 加权波动率（ETF 类型使用默认年化波动率估算）
        BigDecimal weightedVolatility = BigDecimal.ZERO;
        for (PortfolioHolding h : holdings) {
            PortfolioAsset asset = assetMap.get(h.getAssetId());
            if (asset == null || asset.getCurrentPrice() == null) continue;
            BigDecimal marketValue = h.getQuantity().multiply(asset.getCurrentPrice());
            BigDecimal weight = totalMarketValue.compareTo(BigDecimal.ZERO) > 0
                    ? marketValue.divide(totalMarketValue, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // 不同资产类型的默认年化波动率
            BigDecimal assetVol = getDefaultVolatility(asset.getAssetType(), asset.getSymbol());
            weightedVolatility = weightedVolatility.add(weight.multiply(assetVol));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("annualizedVolatility", weightedVolatility.setScale(2, RoundingMode.HALF_UP));
        result.put("method", "weighted-estimated");
        return result;
    }

    private BigDecimal getDefaultVolatility(String assetType, String symbol) {
        // 典型年化波动率估算
        return switch (assetType) {
            case "ETF" -> {
                if (symbol.contains("QQQ") || symbol.contains("NASDAQ")) yield new BigDecimal("22.00");
                if (symbol.contains("VOO") || symbol.contains("SP")) yield new BigDecimal("16.00");
                if (symbol.contains("VTI")) yield new BigDecimal("17.00");
                yield new BigDecimal("18.00");
            }
            case "STOCK" -> new BigDecimal("30.00");
            case "BOND" -> new BigDecimal("6.00");
            case "CASH" -> BigDecimal.ZERO;
            default -> new BigDecimal("18.00");
        };
    }

    // ==================== 资产配置 ====================

    public List<Map<String, Object>> getAssetAllocation(Long portfolioId) {
        List<PortfolioHolding> holdings = holdingMapper.selectList(
                new LambdaQueryWrapper<PortfolioHolding>()
                        .eq(PortfolioHolding::getPortfolioId, portfolioId));

        Map<Long, PortfolioAsset> assetMap = assetMapper.selectList(null).stream()
                .collect(Collectors.toMap(PortfolioAsset::getId, a -> a));

        BigDecimal totalMarketValue = BigDecimal.ZERO;
        List<Map<String, Object>> rows = new ArrayList<>();

        for (PortfolioHolding h : holdings) {
            PortfolioAsset asset = assetMap.get(h.getAssetId());
            if (asset == null || asset.getCurrentPrice() == null) continue;
            BigDecimal mv = h.getQuantity().multiply(asset.getCurrentPrice());
            totalMarketValue = totalMarketValue.add(mv);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("symbol", asset.getSymbol());
            row.put("assetName", asset.getName());
            row.put("assetType", asset.getAssetType());
            row.put("marketValue", mv.setScale(2, RoundingMode.HALF_UP));
            rows.add(row);
        }

        BigDecimal finalTotal = totalMarketValue;
        rows.forEach(row -> {
            BigDecimal mv = (BigDecimal) row.get("marketValue");
            BigDecimal alloc = finalTotal.compareTo(BigDecimal.ZERO) > 0
                    ? mv.multiply(new BigDecimal("100"))
                    .divide(finalTotal, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            row.put("allocationPercent", alloc);
        });

        return rows;
    }

    // ==================== 交易/股息查询 ====================

    public List<PortfolioTrade> getTrades(Long portfolioId) {
        return tradeMapper.selectList(
                new LambdaQueryWrapper<PortfolioTrade>()
                        .eq(PortfolioTrade::getPortfolioId, portfolioId)
                        .orderByDesc(PortfolioTrade::getTradeDate));
    }

    public List<PortfolioDividend> getDividends(Long portfolioId) {
        return dividendMapper.selectList(
                new LambdaQueryWrapper<PortfolioDividend>()
                        .eq(PortfolioDividend::getPortfolioId, portfolioId)
                        .orderByDesc(PortfolioDividend::getDividendDate));
    }

    // ==================== 看板数据汇总 ====================

    public Map<String, Object> getPortfolioDashboard(Long portfolioId) {
        Map<String, Object> dashboard = new LinkedHashMap<>();

        // 组合信息
        Portfolio portfolio = portfolioMapper.selectById(portfolioId);
        dashboard.put("portfolio", portfolio);

        // 持仓汇总
        List<Map<String, Object>> holdings = getHoldingsSummary(portfolioId);
        dashboard.put("holdings", holdings);

        // 总市值
        BigDecimal totalMarketValue = holdings.stream()
                .map(h -> (BigDecimal) h.get("marketValue"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = holdings.stream()
                .map(h -> (BigDecimal) h.get("totalCost"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPnl = totalMarketValue.subtract(totalCost);
        dashboard.put("totalMarketValue", totalMarketValue.setScale(2, RoundingMode.HALF_UP));
        dashboard.put("totalCost", totalCost.setScale(2, RoundingMode.HALF_UP));
        dashboard.put("totalPnl", totalPnl.setScale(2, RoundingMode.HALF_UP));

        // 绩效
        dashboard.put("performance", getPortfolioPerformance(portfolioId));

        // 最大回撤
        dashboard.put("maxDrawdown", getMaxDrawdown(portfolioId));

        // 夏普比率
        dashboard.put("sharpeRatio", getSharpeRatio(portfolioId));

        // 资产配置
        dashboard.put("allocation", getAssetAllocation(portfolioId));

        // 股息收入
        BigDecimal totalDividends = dividendMapper.selectList(
                new LambdaQueryWrapper<PortfolioDividend>()
                        .eq(PortfolioDividend::getPortfolioId, portfolioId))
                .stream()
                .map(PortfolioDividend::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dashboard.put("totalDividends", totalDividends.setScale(2, RoundingMode.HALF_UP));

        return dashboard;
    }
}
