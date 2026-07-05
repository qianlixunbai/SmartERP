package com.smartoa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartoa.common.BusinessException;
import com.smartoa.entity.*;
import com.smartoa.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FiscalPeriodService {

    private final FiscalPeriodMapper fiscalPeriodMapper;
    private final PeriodBalanceMapper periodBalanceMapper;
    private final JournalEntryMapper journalEntryMapper;
    private final AccountMapper accountMapper;
    private final CostCenterMapper costCenterMapper;
    private final ProfitCenterMapper profitCenterMapper;

    public List<FiscalPeriod> getPeriods() {
        return fiscalPeriodMapper.selectList(
                new LambdaQueryWrapper<FiscalPeriod>()
                        .orderByDesc(FiscalPeriod::getYear)
                        .orderByDesc(FiscalPeriod::getMonth));
    }

    public FiscalPeriod getCurrentPeriod() {
        return fiscalPeriodMapper.selectOne(
                new LambdaQueryWrapper<FiscalPeriod>()
                        .eq(FiscalPeriod::getStatus, "OPEN")
                        .orderByDesc(FiscalPeriod::getYear)
                        .orderByDesc(FiscalPeriod::getMonth)
                        .last("LIMIT 1"));
    }

    /**
     * 月结 — 锁定期间 + 生成余额快照
     */
    @Transactional
    public void closePeriod(Integer year, Integer month, Long operatorId) {
        FiscalPeriod period = fiscalPeriodMapper.selectOne(
                new LambdaQueryWrapper<FiscalPeriod>()
                        .eq(FiscalPeriod::getYear, year)
                        .eq(FiscalPeriod::getMonth, month));
        if (period == null) {
            throw new BusinessException("财务期间不存在: " + year + "-" + month);
        }
        if ("CLOSED".equals(period.getStatus())) {
            throw new BusinessException("该期间已关闭");
        }

        // 生成余额快照
        generatePeriodBalances(period);

        // 锁定期间
        period.setStatus("CLOSED");
        period.setClosedBy(operatorId);
        period.setClosedTime(LocalDateTime.now());
        period.setUpdateTime(LocalDateTime.now());
        fiscalPeriodMapper.updateById(period);

        log.info("月结完成: {}-{}, 操作人={}", year, month, operatorId);
    }

    /**
     * 反月结 — 重新打开期间
     */
    @Transactional
    public void reopenPeriod(Integer year, Integer month, Long operatorId) {
        FiscalPeriod period = fiscalPeriodMapper.selectOne(
                new LambdaQueryWrapper<FiscalPeriod>()
                        .eq(FiscalPeriod::getYear, year)
                        .eq(FiscalPeriod::getMonth, month));
        if (period == null) {
            throw new BusinessException("财务期间不存在: " + year + "-" + month);
        }
        if ("OPEN".equals(period.getStatus())) {
            throw new BusinessException("该期间已处于打开状态");
        }

        // 删除该期间的余额快照
        periodBalanceMapper.delete(
                new LambdaQueryWrapper<PeriodBalance>()
                        .eq(PeriodBalance::getPeriodId, period.getId()));

        period.setStatus("OPEN");
        period.setClosedBy(null);
        period.setClosedTime(null);
        period.setUpdateTime(LocalDateTime.now());
        fiscalPeriodMapper.updateById(period);

        log.info("反月结完成: {}-{}, 操作人={}", year, month, operatorId);
    }

    /**
     * 检查期间是否打开 — 记账前调用
     */
    public void checkPeriodOpen(Integer year, Integer month) {
        FiscalPeriod period = fiscalPeriodMapper.selectOne(
                new LambdaQueryWrapper<FiscalPeriod>()
                        .eq(FiscalPeriod::getYear, year)
                        .eq(FiscalPeriod::getMonth, month));
        if (period == null) {
            throw new BusinessException("财务期间不存在: " + year + "-" + month + "，请先创建期间");
        }
        if ("CLOSED".equals(period.getStatus())) {
            throw new BusinessException("财务期间 " + year + "-" + month + " 已关闭，无法记账");
        }
    }

    /**
     * 获取期间余额快照
     */
    public List<PeriodBalance> getPeriodBalances(Long periodId) {
        return periodBalanceMapper.selectList(
                new LambdaQueryWrapper<PeriodBalance>()
                        .eq(PeriodBalance::getPeriodId, periodId));
    }

    /**
     * 财务看板数据
     */
    public Map<String, Object> getDashboardData(Long periodId, Long costCenterId, Long profitCenterId) {
        List<PeriodBalance> balances;
        if (periodId != null) {
            balances = getPeriodBalances(periodId);
        } else {
            // 查最新已关闭期间
            FiscalPeriod latest = fiscalPeriodMapper.selectOne(
                    new LambdaQueryWrapper<FiscalPeriod>()
                            .eq(FiscalPeriod::getStatus, "CLOSED")
                            .orderByDesc(FiscalPeriod::getYear)
                            .orderByDesc(FiscalPeriod::getMonth)
                            .last("LIMIT 1"));
            balances = latest != null ? getPeriodBalances(latest.getId()) : Collections.emptyList();
        }

        // 按筛选条件过滤
        if (costCenterId != null) {
            balances = balances.stream()
                    .filter(b -> costCenterId.equals(b.getCostCenterId()))
                    .collect(Collectors.toList());
        }
        if (profitCenterId != null) {
            balances = balances.stream()
                    .filter(b -> profitCenterId.equals(b.getProfitCenterId()))
                    .collect(Collectors.toList());
        }

        // 汇总
        BigDecimal totalDebit = balances.stream()
                .map(PeriodBalance::getDebitTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCredit = balances.stream()
                .map(PeriodBalance::getCreditTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        // 按科目类型分组
        List<Account> accounts = accountMapper.selectList(null);
        Map<Long, Account> accountMap = accounts.stream()
                .collect(Collectors.toMap(Account::getId, a -> a));

        Map<String, BigDecimal> byType = new LinkedHashMap<>();
        for (PeriodBalance pb : balances) {
            Account acc = accountMap.get(pb.getAccountId());
            if (acc != null) {
                byType.merge(acc.getType(), pb.getClosingBalance(), BigDecimal::add);
            }
        }

        // 按成本中心分组
        Map<String, BigDecimal> byCostCenter = new LinkedHashMap<>();
        Map<Long, CostCenter> ccMap = costCenterMapper.selectList(null).stream()
                .collect(Collectors.toMap(CostCenter::getId, c -> c));
        for (PeriodBalance pb : balances) {
            if (pb.getCostCenterId() != null) {
                CostCenter cc = ccMap.get(pb.getCostCenterId());
                String name = cc != null ? cc.getName() : "CC#" + pb.getCostCenterId();
                byCostCenter.merge(name, pb.getClosingBalance(), BigDecimal::add);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalDebit", totalDebit);
        result.put("totalCredit", totalCredit);
        result.put("balanceCount", balances.size());
        result.put("byAccountType", byType);
        result.put("byCostCenter", byCostCenter);
        return result;
    }

    /**
     * 生成月结余额快照
     */
    private void generatePeriodBalances(FiscalPeriod period) {
        // 查该期间所有分录
        List<JournalEntry> entries = journalEntryMapper.selectList(
                new LambdaQueryWrapper<JournalEntry>()
                        .ge(JournalEntry::getCreateTime,
                                LocalDateTime.of(period.getYear(), period.getMonth(), 1, 0, 0))
                        .lt(JournalEntry::getCreateTime,
                                LocalDateTime.of(period.getYear(), period.getMonth(), 1, 0, 0)
                                        .plusMonths(1)));

        // 按 (accountId, costCenterId, profitCenterId) 分组汇总
        Map<String, List<JournalEntry>> grouped = entries.stream()
                .collect(Collectors.groupingBy(e ->
                        e.getAccountId() + "_" + e.getCostCenterId() + "_" + e.getProfitCenterId()));

        // 获取科目信息用于计算余额方向
        Map<Long, Account> accountMap = accountMapper.selectList(null).stream()
                .collect(Collectors.toMap(Account::getId, a -> a));

        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, List<JournalEntry>> entry : grouped.entrySet()) {
            List<JournalEntry> group = entry.getValue();
            JournalEntry first = group.get(0);

            BigDecimal debitTotal = group.stream().map(JournalEntry::getDebit)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
            BigDecimal creditTotal = group.stream().map(JournalEntry::getCredit)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

            Account acc = accountMap.get(first.getAccountId());
            BigDecimal closingBalance;
            if (acc != null && ("ASSET".equals(acc.getType()) || "EXPENSE".equals(acc.getType()))) {
                closingBalance = debitTotal.subtract(creditTotal).setScale(2, RoundingMode.HALF_UP);
            } else {
                closingBalance = creditTotal.subtract(debitTotal).setScale(2, RoundingMode.HALF_UP);
            }

            PeriodBalance pb = new PeriodBalance();
            pb.setPeriodId(period.getId());
            pb.setAccountId(first.getAccountId());
            pb.setCostCenterId(first.getCostCenterId());
            pb.setProfitCenterId(first.getProfitCenterId());
            pb.setDebitTotal(debitTotal);
            pb.setCreditTotal(creditTotal);
            pb.setClosingBalance(closingBalance);
            pb.setCreateTime(now);
            periodBalanceMapper.insert(pb);
        }
    }
}
