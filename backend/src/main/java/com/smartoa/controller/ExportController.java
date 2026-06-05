package com.smartoa.controller;

import com.smartoa.entity.User;
import com.smartoa.service.ExportService;
import com.smartoa.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;
    private final UserService userService;

    @GetMapping("/api/export/leaves")
    public ResponseEntity<byte[]> exportLeaves() {
        User user = userService.getLoginUser();
        if (user == null || !"MANAGER".equals(user.getRole())) {
            return ResponseEntity.status(403).build();
        }
        byte[] data = exportService.exportLeaveRequests();
        String filename = "请假单导出_" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
