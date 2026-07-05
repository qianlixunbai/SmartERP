package com.smartoa.controller;

import com.smartoa.common.BusinessException;
import com.smartoa.common.Result;
import com.smartoa.dto.AssetPriceUpdate;
import com.smartoa.dto.DividendRequest;
import com.smartoa.dto.TradeRequest;
import com.smartoa.entity.*;
import com.smartoa.service.PortfolioService;
import com.smartoa.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final UserService userService;

    // ==================== 组合管理 ====================

    @GetMapping("/api/portfolio/portfolios")
    public Result<List<Portfolio>> getPortfolios() {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        return Result.success(portfolioService.getPortfolios());
    }

    @PostMapping("/api/portfolio/portfolios")
    public Result<Portfolio> createPortfolio(@RequestBody Map<String, String> body) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        if (!"MANAGER".equals(user.getRole())) throw new BusinessException(403, "无权限");
        return Result.success(portfolioService.createPortfolio(
                body.get("name"), body.get("description"), body.get("baseCurrency")));
    }

    // ==================== 资产标的 ====================

    @GetMapping("/api/portfolio/assets")
    public Result<List<PortfolioAsset>> getAssets() {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        return Result.success(portfolioService.getAssets());
    }

    @PostMapping("/api/portfolio/assets")
    public Result<PortfolioAsset> createAsset(@RequestBody Map<String, String> body) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        if (!"MANAGER".equals(user.getRole())) throw new BusinessException(403, "无权限");
        return Result.success(portfolioService.createAsset(
                body.get("symbol"), body.get("name"), body.get("assetType"), body.get("currency")));
    }

    @PutMapping("/api/portfolio/assets/{id}/price")
    public Result<Void> updateAssetPrice(@PathVariable Long id, @RequestBody AssetPriceUpdate dto) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        if (!"MANAGER".equals(user.getRole())) throw new BusinessException(403, "无权限");
        portfolioService.updateAssetPrice(dto.getSymbol(), dto.getCurrentPrice());
        return Result.success(null, "价格已更新");
    }

    // ==================== 持仓 ====================

    @GetMapping("/api/portfolio/holdings")
    public Result<List<Map<String, Object>>> getHoldings(@RequestParam Long portfolioId) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        return Result.success(portfolioService.getHoldingsSummary(portfolioId));
    }

    // ==================== 交易 ====================

    @GetMapping("/api/portfolio/trades")
    public Result<List<PortfolioTrade>> getTrades(@RequestParam Long portfolioId) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        return Result.success(portfolioService.getTrades(portfolioId));
    }

    @PostMapping("/api/portfolio/trades")
    public Result<PortfolioTrade> executeTrade(@RequestBody TradeRequest dto) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        if (!"MANAGER".equals(user.getRole())) throw new BusinessException(403, "无权限");
        return Result.success(portfolioService.executeTrade(dto, user.getId()));
    }

    // ==================== 股息 ====================

    @GetMapping("/api/portfolio/dividends")
    public Result<List<PortfolioDividend>> getDividends(@RequestParam Long portfolioId) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        return Result.success(portfolioService.getDividends(portfolioId));
    }

    @PostMapping("/api/portfolio/dividends")
    public Result<PortfolioDividend> recordDividend(@RequestBody DividendRequest dto) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        if (!"MANAGER".equals(user.getRole())) throw new BusinessException(403, "无权限");
        return Result.success(portfolioService.recordDividend(dto));
    }

    // ==================== 分析 ====================

    @GetMapping("/api/portfolio/allocation")
    public Result<List<Map<String, Object>>> getAllocation(@RequestParam Long portfolioId) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        return Result.success(portfolioService.getAssetAllocation(portfolioId));
    }

    @GetMapping("/api/portfolio/performance")
    public Result<Map<String, Object>> getPerformance(@RequestParam Long portfolioId) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        return Result.success(portfolioService.getPortfolioPerformance(portfolioId));
    }

    @GetMapping("/api/portfolio/dashboard")
    public Result<Map<String, Object>> getDashboard(@RequestParam Long portfolioId) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        return Result.success(portfolioService.getPortfolioDashboard(portfolioId));
    }
}
