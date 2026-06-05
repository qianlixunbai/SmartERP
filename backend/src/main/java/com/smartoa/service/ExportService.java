package com.smartoa.service;

import com.smartoa.entity.LeaveRequest;
import com.smartoa.entity.User;
import com.smartoa.mapper.LeaveRequestMapper;
import com.smartoa.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final LeaveRequestMapper leaveRequestMapper;
    private final UserMapper userMapper;

    public byte[] exportLeaveRequests() {
        List<LeaveRequest> requests = leaveRequestMapper.selectList(null);
        List<User> users = userMapper.selectList(null);

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("请假单");

        Row header = sheet.createRow(0);
        String[] cols = {"ID", "申请人", "请假类型", "开始日期", "结束日期", "原因", "状态", "审批步骤", "创建时间"};
        for (int i = 0; i < cols.length; i++) {
            header.createCell(i).setCellValue(cols[i]);
        }

        for (int i = 0; i < requests.size(); i++) {
            LeaveRequest r = requests.get(i);
            String applicantName = users.stream()
                    .filter(u -> u.getId().equals(r.getApplicantId()))
                    .findFirst().map(User::getRealName).orElse("未知");

            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(r.getId() != null ? r.getId() : 0);
            row.createCell(1).setCellValue(applicantName);
            row.createCell(2).setCellValue(r.getLeaveType());
            row.createCell(3).setCellValue(r.getStartDate() != null ? r.getStartDate().toString() : "");
            row.createCell(4).setCellValue(r.getEndDate() != null ? r.getEndDate().toString() : "");
            row.createCell(5).setCellValue(r.getReason() != null ? r.getReason() : "");
            row.createCell(6).setCellValue(r.getStatus());
            row.createCell(7).setCellValue(r.getApprovalStep() != null ? r.getApprovalStep() : 0);
            row.createCell(8).setCellValue(r.getCreateTime() != null ? r.getCreateTime().toString() : "");
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            wb.write(bos);
            wb.close();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("导出失败", e);
        }
    }
}
