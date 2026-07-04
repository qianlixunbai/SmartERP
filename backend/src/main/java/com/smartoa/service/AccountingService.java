package com.smartoa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartoa.common.BusinessException;
import com.smartoa.entity.Account;
import com.smartoa.entity.AuditLog;
import com.smartoa.entity.JournalEntry;
import com.smartoa.mapper.AccountMapper;
import com.smartoa.mapper.AuditLogMapper;
import com.smartoa.mapper.JournalEntryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountingService {

    private final JournalEntryMapper journalEntryMapper;
    private final AccountMapper accountMapper;
    private final AuditLogMapper auditLogMapper;

    /**
     * 经费入账 — 复式记账
     * 借：费用科目（办公费/差旅费/…）
     * 贷：其他应收款
     *
     * @return transactionId（UUID）
     */
    @Transactional
    public String post(Long expenseRequestId, String category, BigDecimal amount,
                       Long operatorId, String memo) {
        BigDecimal amt = amount.setScale(2, RoundingMode.HALF_UP);
        if (amt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("入账金额必须大于0");
        }

        // 查费用科目
        String expenseCode = mapCategoryToCode(category);
        Account expenseAccount = accountMapper.selectOne(
                new LambdaQueryWrapper<Account>().eq(Account::getCode, expenseCode));
        if (expenseAccount == null) {
            throw new BusinessException("费用科目不存在: " + expenseCode);
        }

        // 查其他应收款（贷方）
        Account receivableAccount = accountMapper.selectOne(
                new LambdaQueryWrapper<Account>().eq(Account::getCode, "1221"));
        if (receivableAccount == null) {
            throw new BusinessException("科目1221（其他应收款）不存在");
        }

        String txnId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        // 借：费用科目
        JournalEntry debit = new JournalEntry();
        debit.setTransactionId(txnId);
        debit.setAccountId(expenseAccount.getId());
        debit.setDebit(amt);
        debit.setCredit(BigDecimal.ZERO.setScale(2));
        debit.setMemo(memo != null ? memo : "经费报销-" + category);
        debit.setCreatedBy(operatorId);
        debit.setCreateTime(now);
        journalEntryMapper.insert(debit);

        // 贷：其他应收款
        JournalEntry credit = new JournalEntry();
        credit.setTransactionId(txnId);
        credit.setAccountId(receivableAccount.getId());
        credit.setDebit(BigDecimal.ZERO.setScale(2));
        credit.setCredit(amt);
        credit.setMemo(memo != null ? memo : "经费报销-" + category);
        credit.setCreatedBy(operatorId);
        credit.setCreateTime(now);
        journalEntryMapper.insert(credit);

        // 审计日志
        writeAuditLog("POST", "EXPENSE", expenseRequestId, operatorId,
                "transactionId=" + txnId + ", amount=" + amt + ", category=" + category);

        log.info("入账成功: txnId={}, 借:{} {}, 贷:{} {}",
                txnId, expenseAccount.getName(), amt, receivableAccount.getName(), amt);
        return txnId;
    }

    /**
     * 红字冲销 — 生成借贷互换的反向分录
     */
    @Transactional
    public String reverse(String originalTransactionId, Long operatorId, String reason) {
        List<JournalEntry> originals = journalEntryMapper.selectList(
                new LambdaQueryWrapper<JournalEntry>()
                        .eq(JournalEntry::getTransactionId, originalTransactionId));

        if (originals.isEmpty()) {
            throw new BusinessException("原始分录不存在: " + originalTransactionId);
        }

        // 检查是否已冲销
        List<JournalEntry> existing = journalEntryMapper.selectList(
                new LambdaQueryWrapper<JournalEntry>()
                        .like(JournalEntry::getMemo, "冲销#" + originalTransactionId));
        if (!existing.isEmpty()) {
            throw new BusinessException("该分录已被冲销");
        }

        String newTxnId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        for (JournalEntry orig : originals) {
            JournalEntry reversal = new JournalEntry();
            reversal.setTransactionId(newTxnId);
            reversal.setAccountId(orig.getAccountId());
            reversal.setDebit(orig.getCredit());   // 借贷互换
            reversal.setCredit(orig.getDebit());
            reversal.setMemo("冲销#" + originalTransactionId + " 原因:" + reason);
            reversal.setCreatedBy(operatorId);
            reversal.setCreateTime(now);
            journalEntryMapper.insert(reversal);
        }

        writeAuditLog("REVERSE", "JOURNAL", 0L, operatorId,
                "originalTxnId=" + originalTransactionId + ", newTxnId=" + newTxnId + ", reason=" + reason);

        log.info("冲销成功: 原始={}, 新txnId={}, 原因={}", originalTransactionId, newTxnId, reason);
        return newTxnId;
    }

    /**
     * 试算平衡 — 校验所有分录 SUM(debit) == SUM(credit)
     */
    public Map<String, Object> trialBalance() {
        List<JournalEntry> all = journalEntryMapper.selectList(null);

        BigDecimal totalDebit = all.stream()
                .map(JournalEntry::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalCredit = all.stream()
                .map(JournalEntry::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        boolean balanced = totalDebit.compareTo(totalCredit) == 0;

        return Map.of(
                "totalDebit", totalDebit,
                "totalCredit", totalCredit,
                "balanced", balanced,
                "entryCount", all.size()
        );
    }

    /**
     * 按科目汇总余额
     */
    public List<Map<String, Object>> accountBalances() {
        List<Account> accounts = accountMapper.selectList(
                new LambdaQueryWrapper<Account>().eq(Account::getActive, true));
        List<JournalEntry> entries = journalEntryMapper.selectList(null);

        Map<Long, BigDecimal> debitMap = entries.stream()
                .collect(Collectors.groupingBy(
                        JournalEntry::getAccountId,
                        Collectors.reducing(BigDecimal.ZERO, JournalEntry::getDebit, BigDecimal::add)));

        Map<Long, BigDecimal> creditMap = entries.stream()
                .collect(Collectors.groupingBy(
                        JournalEntry::getAccountId,
                        Collectors.reducing(BigDecimal.ZERO, JournalEntry::getCredit, BigDecimal::add)));

        return accounts.stream().map(acc -> {
            BigDecimal dr = debitMap.getOrDefault(acc.getId(), BigDecimal.ZERO).setScale(2);
            BigDecimal cr = creditMap.getOrDefault(acc.getId(), BigDecimal.ZERO).setScale(2);
            BigDecimal balance;
            if ("ASSET".equals(acc.getType()) || "EXPENSE".equals(acc.getType())) {
                balance = dr.subtract(cr).setScale(2);
            } else {
                balance = cr.subtract(dr).setScale(2);
            }
            return Map.<String, Object>of(
                    "code", acc.getCode(),
                    "name", acc.getName(),
                    "type", acc.getType(),
                    "debit", dr,
                    "credit", cr,
                    "balance", balance
            );
        }).collect(Collectors.toList());
    }

    /**
     * 根据经费类别映射到科目编码
     */
    private String mapCategoryToCode(String category) {
        return switch (category) {
            case "办公" -> "6001";
            case "差旅" -> "6002";
            case "招待" -> "6003";
            case "交通" -> "6004";
            default -> "6005";
        };
    }

    private void writeAuditLog(String action, String targetType, Long targetId,
                               Long actorId, String detail) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setActorId(actorId);
        log.setDetail(detail);
        log.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(log);
    }
}
