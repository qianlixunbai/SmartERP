package com.smartoa.controller;

import com.smartoa.common.BusinessException;
import com.smartoa.common.Result;
import com.smartoa.config.JwtUtil;
import com.smartoa.dto.LoginRequest;
import com.smartoa.entity.User;
import com.smartoa.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/api/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest dto) {
        User user = userService.login(dto.getUsername(), dto.getPassword());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("realName", user.getRealName());
        userMap.put("role", user.getRole());
        userMap.put("department", user.getDepartment());
        userMap.put("directLeaderId", user.getDirectLeaderId());
        userMap.put("departmentHeadId", user.getDepartmentHeadId());
        Map<String, Object> data = Map.of("token", token, "user", userMap);
        return Result.success(data);
    }

    @GetMapping("/api/user/current")
    public Result<Map<String, Object>> currentUser() {
        User user = userService.getLoginUser();
        if (user == null) {
            throw new BusinessException(401, "未登录");
        }
        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("realName", user.getRealName());
        userMap.put("role", user.getRole());
        userMap.put("department", user.getDepartment());
        userMap.put("directLeaderId", user.getDirectLeaderId());
        userMap.put("departmentHeadId", user.getDepartmentHeadId());
        return Result.success(userMap);
    }

    @PostMapping("/api/logout")
    public Result<Void> logout() {
        return Result.success(null);
    }

    @GetMapping("/api/users/list")
    public Result<List<User>> listUsers() {
        return Result.success(userService.listAll());
    }
}
