package com.smartoa.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LeaveSubmitRequest {
    private Long templateId;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}
