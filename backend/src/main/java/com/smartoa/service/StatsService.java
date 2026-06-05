package com.smartoa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartoa.entity.ApprovalRecord;
import com.smartoa.entity.ApprovalTemplate;
import com.smartoa.entity.LeaveRequest;
import com.smartoa.mapper.ApprovalRecordMapper;
import com.smartoa.mapper.ApprovalTemplateMapper;
import com.smartoa.mapper.LeaveRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final LeaveRequestMapper leaveRequestMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final ApprovalTemplateMapper templateMapper;

    /**
     * 各模板平均审批时长（分钟）
     */
    public List<Map<String, Object>> avgApprovalDuration() {
        List<LeaveRequest> approved = leaveRequestMapper.selectList(
                new LambdaQueryWrapper<LeaveRequest>().eq(LeaveRequest::getStatus, "APPROVED"));

        Map<Long, List<Double>> templateDurations = new LinkedHashMap<>();
        for (LeaveRequest r : approved) {
            List<ApprovalRecord> records = approvalRecordMapper.selectList(
                    new LambdaQueryWrapper<ApprovalRecord>()
                            .eq(ApprovalRecord::getLeaveRequestId, r.getId())
                            .orderByAsc(ApprovalRecord::getCreateTime));
            if (records.isEmpty()) continue;

            LocalDateTime firstAction = records.get(0).getCreateTime();
            double minutes = Duration.between(firstAction, records.get(records.size() - 1).getCreateTime()).toSeconds() / 60.0;
            Long templateId = r.getTemplateId() != null ? r.getTemplateId() : 0;
            templateDurations.computeIfAbsent(templateId, k -> new ArrayList<>()).add(minutes);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<Double>> entry : templateDurations.entrySet()) {
            String templateName = "(未关联模板)";
            if (entry.getKey() != 0) {
                ApprovalTemplate t = templateMapper.selectById(entry.getKey());
                if (t != null) templateName = t.getName();
            }
            double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("templateId", entry.getKey());
            item.put("templateName", templateName);
            item.put("avgMinutes", Math.round(avg * 10.0) / 10.0);
            item.put("count", entry.getValue().size());
            result.add(item);
        }
        return result;
    }

    /**
     * 各模板使用量
     */
    public List<Map<String, Object>> templateUsage() {
        List<LeaveRequest> all = leaveRequestMapper.selectList(null);
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (LeaveRequest r : all) {
            Long tid = r.getTemplateId() != null ? r.getTemplateId() : 0;
            counts.merge(tid, 1L, Long::sum);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : counts.entrySet()) {
            String templateName = "(未关联模板)";
            if (entry.getKey() != 0) {
                ApprovalTemplate t = templateMapper.selectById(entry.getKey());
                if (t != null) templateName = t.getName();
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("templateId", entry.getKey());
            item.put("templateName", templateName);
            item.put("count", entry.getValue());
            result.add(item);
        }
        return result;
    }
}
