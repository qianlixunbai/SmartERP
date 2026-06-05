package com.smartoa.controller;

import com.smartoa.common.Result;
import com.smartoa.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/api/stats/avg-duration")
    public Result<List<Map<String, Object>>> avgDuration() {
        return Result.success(statsService.avgApprovalDuration());
    }

    @GetMapping("/api/stats/template-usage")
    public Result<List<Map<String, Object>>> templateUsage() {
        return Result.success(statsService.templateUsage());
    }
}
