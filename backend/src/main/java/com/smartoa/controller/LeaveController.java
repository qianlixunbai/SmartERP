package com.smartoa.controller;

import com.smartoa.common.BusinessException;
import com.smartoa.common.Result;
import com.smartoa.dto.LeaveApproveRequest;
import com.smartoa.dto.LeaveSubmitRequest;
import com.smartoa.dto.LeaveTransferRequest;
import com.smartoa.entity.ApprovalRecord;
import com.smartoa.entity.ApprovalTask;
import com.smartoa.entity.LeaveRequest;
import com.smartoa.entity.User;
import com.smartoa.service.ApprovalAuthorizationService;
import com.smartoa.service.LeaveService;
import com.smartoa.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class LeaveController {

    private final LeaveService leaveService;
    private final UserService userService;
    private final ApprovalAuthorizationService approvalAuthorizationService;

    @PostMapping("/api/leave/submit")
    public Result<Void> submitLeave(@RequestBody @Valid LeaveSubmitRequest dto) {
        User user = userService.getLoginUser();
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        leaveService.submitLeave(user.getId(), dto);
        return Result.success(null, "提交成功");
    }

    @PostMapping("/api/leave/approve")
    public Result<Void> approveLeave(@RequestBody @Valid LeaveApproveRequest dto) {
        User user = userService.getLoginUser();
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        leaveService.approveLeave(dto.getRequestId(), user.getId(), dto.getAction(), dto.getComment());
        return Result.success(null, "操作成功");
    }

    @PostMapping("/api/leave/{id}/withdraw")
    public Result<Void> withdrawLeave(@PathVariable @Positive(message = "ID必须为正整数") Long id) {
        User user = userService.getLoginUser();
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        leaveService.withdrawLeave(id, user.getId());
        return Result.success(null, "撤回成功");
    }

    @PostMapping("/api/leave/{id}/transfer")
    public Result<Void> transferLeave(@PathVariable @Positive(message = "ID必须为正整数") Long id,
                                       @RequestBody @Valid LeaveTransferRequest dto) {
        User user = userService.getLoginUser();
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        leaveService.transferLeave(id, user.getId(), dto.getToUserId());
        return Result.success(null, "转派成功");
    }

    @PostMapping("/api/leave/repair")
    public Result<Integer> repairStuckRequests() {
        User user = userService.getLoginUser();
        if (user == null || user.getId() == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!"MANAGER".equals(user.getRole())) {
            throw new BusinessException(403, "无权限");
        }
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
        User user = userService.getLoginUser();
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        approvalAuthorizationService.requireReadableLeave(id, user);
        return Result.success(leaveService.getRequestDetail(id));
    }

    @GetMapping("/api/leave/{id}/records")
    public Result<List<ApprovalRecord>> getApprovalRecords(@PathVariable Long id) {
        User user = userService.getLoginUser();
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        approvalAuthorizationService.requireReadableLeave(id, user);
        return Result.success(leaveService.getApprovalRecords(id));
    }

    @GetMapping("/api/leave/{id}/tasks")
    public Result<List<ApprovalTask>> getPendingTasks(@PathVariable Long id) {
        User user = userService.getLoginUser();
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        approvalAuthorizationService.requireReadableLeave(id, user);
        return Result.success(leaveService.getPendingTasks(id));
    }
}
