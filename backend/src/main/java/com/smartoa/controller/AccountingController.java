package com.smartoa.controller;

import com.smartoa.common.BusinessException;
import com.smartoa.common.Result;
import com.smartoa.dto.CostCenterRequest;
import com.smartoa.dto.PeriodCloseRequest;
import com.smartoa.dto.ProfitCenterRequest;
import com.smartoa.entity.*;
import com.smartoa.service.FiscalPeriodService;
import com.smartoa.service.UserService;
import com.smartoa.mapper.CostCenterMapper;
import com.smartoa.mapper.ProfitCenterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AccountingController {

    private final FiscalPeriodService fiscalPeriodService;
    private final UserService userService;
    private final CostCenterMapper costCenterMapper;
    private final ProfitCenterMapper profitCenterMapper;

    // ========== 成本中心 CRUD ==========

    @GetMapping("/api/accounting/cost-centers")
    public Result<List<CostCenter>> getCostCenters() {
        return Result.success(costCenterMapper.selectList(null));
    }

    @PostMapping("/api/accounting/cost-centers")
    public Result<CostCenter> createCostCenter(@RequestBody CostCenterRequest dto) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        if (!"MANAGER".equals(user.getRole())) throw new BusinessException(403, "无权限");

        CostCenter cc = new CostCenter();
        cc.setCode(dto.getCode());
        cc.setName(dto.getName());
        cc.setDescription(dto.getDescription());
        cc.setActive(true);
        cc.setCreateTime(LocalDateTime.now());
        cc.setUpdateTime(LocalDateTime.now());
        costCenterMapper.insert(cc);
        return Result.success(cc);
    }

    @PutMapping("/api/accounting/cost-centers/{id}")
    public Result<CostCenter> updateCostCenter(@PathVariable Long id, @RequestBody CostCenterRequest dto) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        if (!"MANAGER".equals(user.getRole())) throw new BusinessException(403, "无权限");

        CostCenter cc = costCenterMapper.selectById(id);
        if (cc == null) throw new BusinessException("成本中心不存在");
        cc.setCode(dto.getCode());
        cc.setName(dto.getName());
        cc.setDescription(dto.getDescription());
        cc.setUpdateTime(LocalDateTime.now());
        costCenterMapper.updateById(cc);
        return Result.success(cc);
    }

    // ========== 利润中心 CRUD ==========

    @GetMapping("/api/accounting/profit-centers")
    public Result<List<ProfitCenter>> getProfitCenters() {
        return Result.success(profitCenterMapper.selectList(null));
    }

    @PostMapping("/api/accounting/profit-centers")
    public Result<ProfitCenter> createProfitCenter(@RequestBody ProfitCenterRequest dto) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        if (!"MANAGER".equals(user.getRole())) throw new BusinessException(403, "无权限");

        ProfitCenter pc = new ProfitCenter();
        pc.setCode(dto.getCode());
        pc.setName(dto.getName());
        pc.setDescription(dto.getDescription());
        pc.setActive(true);
        pc.setCreateTime(LocalDateTime.now());
        pc.setUpdateTime(LocalDateTime.now());
        profitCenterMapper.insert(pc);
        return Result.success(pc);
    }

    @PutMapping("/api/accounting/profit-centers/{id}")
    public Result<ProfitCenter> updateProfitCenter(@PathVariable Long id, @RequestBody ProfitCenterRequest dto) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        if (!"MANAGER".equals(user.getRole())) throw new BusinessException(403, "无权限");

        ProfitCenter pc = profitCenterMapper.selectById(id);
        if (pc == null) throw new BusinessException("利润中心不存在");
        pc.setCode(dto.getCode());
        pc.setName(dto.getName());
        pc.setDescription(dto.getDescription());
        pc.setUpdateTime(LocalDateTime.now());
        profitCenterMapper.updateById(pc);
        return Result.success(pc);
    }

    // ========== 财务期间 ==========

    @GetMapping("/api/accounting/periods")
    public Result<List<FiscalPeriod>> getPeriods() {
        return Result.success(fiscalPeriodService.getPeriods());
    }

    @PostMapping("/api/accounting/periods/close")
    public Result<Void> closePeriod(@RequestBody PeriodCloseRequest dto) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        if (!"MANAGER".equals(user.getRole())) throw new BusinessException(403, "无权限");
        fiscalPeriodService.closePeriod(dto.getYear(), dto.getMonth(), user.getId());
        return Result.success(null, "月结完成");
    }

    @PostMapping("/api/accounting/periods/reopen")
    public Result<Void> reopenPeriod(@RequestBody PeriodCloseRequest dto) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        if (!"MANAGER".equals(user.getRole())) throw new BusinessException(403, "无权限");
        fiscalPeriodService.reopenPeriod(dto.getYear(), dto.getMonth(), user.getId());
        return Result.success(null, "反月结完成");
    }

    // ========== 期间余额快照 ==========

    @GetMapping("/api/accounting/period-balances/{periodId}")
    public Result<List<PeriodBalance>> getPeriodBalances(@PathVariable Long periodId) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        if (!"MANAGER".equals(user.getRole())) throw new BusinessException(403, "无权限");
        return Result.success(fiscalPeriodService.getPeriodBalances(periodId));
    }

    // ========== 财务看板 ==========

    @GetMapping("/api/accounting/dashboard")
    public Result<Map<String, Object>> dashboard(
            @RequestParam(required = false) Long periodId,
            @RequestParam(required = false) Long costCenterId,
            @RequestParam(required = false) Long profitCenterId) {
        User user = userService.getLoginUser();
        if (user == null) throw new BusinessException(401, "请先登录");
        if (!"MANAGER".equals(user.getRole())) throw new BusinessException(403, "无权限");
        return Result.success(fiscalPeriodService.getDashboardData(periodId, costCenterId, profitCenterId));
    }
}
