package com.smartoa.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeaveTransferRequest {
    @NotNull(message = "转派目标用户ID不能为空")
    private Long toUserId;
}
