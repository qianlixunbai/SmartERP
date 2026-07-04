package com.smartoa.dto;

import lombok.Data;

@Data
public class ExpenseApproveRequest {
    private Long requestId;
    private String action;
    private String comment;
}
