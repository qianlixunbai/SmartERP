package com.smartoa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LeaveApproveRequest {
    @NotNull(message = "申请ID不能为空")
    @Positive(message = "申请ID必须为正整数")
    private Long requestId;

    @NotBlank(message = "审批动作不能为空")
    @Pattern(regexp = "APPROVE|REJECT", message = "审批动作只能为APPROVE或REJECT")
    private String action;

    @Size(max = 500, message = "审批意见不能超过500字")
    private String comment;
}
