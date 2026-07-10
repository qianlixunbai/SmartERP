package com.smartoa.controller;

import com.smartoa.common.BusinessException;
import com.smartoa.common.Result;
import com.smartoa.dto.*;
import com.smartoa.entity.*;
import com.smartoa.service.PortfolioService;
import com.smartoa.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Validated
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final UserService userService;

    /**
     * v4.0.1 暂无投资组合所有权模型，Portfolio 模块按组织级敏感财务数据处理，仅 MANAGER 可访问。
     */
    private User requireManager() {
        User user = userService.getLoginUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!"MANAGER".equals(user.getRole())) {
            throw new BusinessException(403, "无权限");
        }
        return user;
    }

    // ==================== 组合管理 ====================

    @GetMapping("/api/portfolio/portfolios")
    public Result<List<Portfolio>> getPortfolios() {
        requireManager();
        return Result.success(portfolioService.getPortfolios());
    }

    @PostMapping("/api/portfolio/portfolios")
    public Result<Portfolio> createPortfolio(@RequestBody @Valid PortfolioCreateRequest dto) {
        requireManager();
        return Result.success(portfolioService.createPortfolio(
                dto.getName(), dto.getDescription(), dto.getBaseCurrency()));
    }

    // ==================== 资产标的 ====================

    @GetMapping("/api/portfolio/assets")
    public Result<List<PortfolioAsset>> getAssets() {
        requireManager();
        return Result.success(portfolioService.getAssets());
    }

    @PostMapping("/api/portfolio/assets")
    public Result<PortfolioAsset> createAsset(@RequestBody @Valid PortfolioAssetCreateRequest dto) {
        requireManager();
        return Result.success(portfolioService.createAsset(
                dto.getSymbol(), dto.getName(), dto.getAssetType(), dto.getCurrency()));
    }

    @PutMapping("/api/portfolio/assets/{id}/price")
    public Result<Void> updateAssetPrice(@PathVariable @Positive(message = "ID必须为正整数") Long id,
                                          @RequestBody @Valid AssetPriceUpdate dto) {
        requireManager();
        portfolioService.updateAssetPrice(id, dto.getCurrentPrice());
        return Result.success(null, "价格已更新");
    }

    // ==================== 持仓 ====================

    @GetMapping("/api/portfolio/holdings")
    public Result<List<Map<String, Object>>> getHoldings(@RequestParam Long portfolioId) {
        requireManager();
        return Result.success(portfolioService.getHoldingsSummary(portfolioId));
    }

    // ==================== 交易 ====================

    @GetMapping("/api/portfolio/trades")
    public Result<List<PortfolioTrade>> getTrades(@RequestParam Long portfolioId) {
        requireManager();
        return Result.success(portfolioService.getTrades(portfolioId));
    }

    @PostMapping("/api/portfolio/trades")
    public Result<PortfolioTrade> executeTrade(@RequestBody @Valid TradeRequest dto) {
        User user = requireManager();
        return Result.success(portfolioService.executeTrade(dto, user.getId()));
    }

    // ==================== 股息 ====================

    @GetMapping("/api/portfolio/dividends")
    public Result<List<PortfolioDividend>> getDividends(@RequestParam Long portfolioId) {
        requireManager();
        return Result.success(portfolioService.getDividends(portfolioId));
    }

    @PostMapping("/api/portfolio/dividends")
    public Result<PortfolioDividend> recordDividend(@RequestBody @Valid DividendRequest dto) {
        requireManager();
        return Result.success(portfolioService.recordDividend(dto));
    }

    // ==================== 分析 ====================

    @GetMapping("/api/portfolio/allocation")
    public Result<List<Map<String, Object>>> getAllocation(@RequestParam Long portfolioId) {
        requireManager();
        return Result.success(portfolioService.getAssetAllocation(portfolioId));
    }

    @GetMapping("/api/portfolio/performance")
    public Result<Map<String, Object>> getPerformance(@RequestParam Long portfolioId) {
        requireManager();
        return Result.success(portfolioService.getPortfolioPerformance(portfolioId));
    }

    @GetMapping("/api/portfolio/dashboard")
    public Result<Map<String, Object>> getDashboard(@RequestParam Long portfolioId) {
        requireManager();
        return Result.success(portfolioService.getPortfolioDashboard(portfolioId));
    }
}
