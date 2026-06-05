package com.smartoa.controller;

import com.smartoa.common.BusinessException;
import com.smartoa.common.Result;
import com.smartoa.dto.LeaveSubmitRequest;
import com.smartoa.entity.ApprovalRecord;
import com.smartoa.entity.ApprovalTask;
import com.smartoa.entity.LeaveRequest;
import com.smartoa.entity.User;
import com.smartoa.service.LeaveService;
import com.smartoa.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;
    private final UserService userService;

    @PostMapping("/api/leave/submit")
    public Result<Void> submitLeave(@RequestBody LeaveSubmitRequest dto) {
        User user = userService.getLoginUser();
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (user.getDirectLeaderId() == null) {
            throw new BusinessException("您尚未分配直属领导，无法提交");
        }
        leaveService.submitLeave(user.getId(), dto);
        return Result.success(null, "提交成功");
    }

    @PostMapping("/api/leave/approve")
    public Result<Void> approveLeave(@RequestBody Map<String, String> body) {
        User user = userService.getLoginUser();
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        Long requestId = Long.valueOf(body.get("requestId"));
        String action = body.get("action");
        String comment = body.getOrDefault("comment", "");
        leaveService.approveLeave(requestId, user.getId(), action, comment);
        return Result.success(null, "操作成功");
    }

    @PostMapping("/api/leave/{id}/withdraw")
    public Result<Void> withdrawLeave(@PathVariable Long id) {
        User user = userService.getLoginUser();
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        leaveService.withdrawLeave(id, user.getId());
        return Result.success(null, "撤回成功");
    }

    @PostMapping("/api/leave/{id}/transfer")
    public Result<Void> transferLeave(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        User user = userService.getLoginUser();
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        Long toUserId = body.get("toUserId");
        leaveService.transferLeave(id, user.getId(), toUserId);
        return Result.success(null, "转派成功");
    }

    @PostMapping("/api/leave/repair")
    public Result<Integer> repairStuckRequests() {
        int count = leaveService.repairStuckRequests();
        return Result.success(count, "已修复 " + count + " 条滞留申请");
    }

    @GetMapping("/api/leave/all")
    public Result<List<LeaveRequest>> getAllRequests() {
        User user = userService.getLoginUser();
        if (user == null || !"MANAGER".equals(user.getRole())) {
            throw new BusinessException(403, "无权限");
        }
        return Result.success(leaveService.getAllRequests());
    }

    @GetMapping("/api/leave/my-requests")
    public Result<List<LeaveRequest>> getMyRequests() {
        User user = userService.getLoginUser();
        return Result.success(leaveService.getMyRequests(user.getId()));
    }

    @GetMapping("/api/leave/pending")
    public Result<List<LeaveRequest>> getPendingRequests() {
        User user = userService.getLoginUser();
        return Result.success(leaveService.getPendingRequests(user.getId()));
    }

    @GetMapping("/api/leave/done")
    public Result<List<LeaveRequest>> getDoneRequests() {
        User user = userService.getLoginUser();
        return Result.success(leaveService.getDoneRequests(user.getId()));
    }

    @GetMapping("/api/leave/{id}")
    public Result<LeaveRequest> getRequestDetail(@PathVariable Long id) {
        return Result.success(leaveService.getRequestDetail(id));
    }

    @GetMapping("/api/leave/{id}/records")
    public Result<List<ApprovalRecord>> getApprovalRecords(@PathVariable Long id) {
        return Result.success(leaveService.getApprovalRecords(id));
    }

    @GetMapping("/api/leave/{id}/tasks")
    public Result<List<ApprovalTask>> getPendingTasks(@PathVariable Long id) {
        return Result.success(leaveService.getPendingTasks(id));
    }
}
