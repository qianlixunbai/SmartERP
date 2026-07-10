package com.smartoa.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class LeaveTransferRequest {
    @NotNull(message = "转派目标用户ID不能为空")
    @Positive(message = "转派目标用户ID必须为正整数")
    private Long toUserId;
}
