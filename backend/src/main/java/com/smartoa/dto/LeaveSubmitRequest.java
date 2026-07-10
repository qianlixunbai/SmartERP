package com.smartoa.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveSubmitRequest {
    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    @NotBlank(message = "请假类型不能为空")
    @Size(max = 20, message = "请假类型不能超过20字")
    private String leaveType;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @NotBlank(message = "请假原因不能为空")
    @Size(max = 500, message = "请假原因不能超过500字")
    private String reason;

    @AssertTrue(message = "结束日期不能早于开始日期")
    @JsonIgnore
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate);
    }
}
