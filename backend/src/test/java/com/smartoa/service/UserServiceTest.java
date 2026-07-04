package com.smartoa.service;

import com.smartoa.common.BusinessException;
import com.smartoa.entity.User;
import com.smartoa.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Test
    @DisplayName("登录成功 - 正确的用户名和密码")
    void testLogin_Success() {
        // When: 使用正确的账号密码登录
        User user = userService.login("admin", "123456");

        // Then: 应该返回用户信息
        assertNotNull(user, "登录结果不应为空");
        assertEquals("admin", user.getUsername());
        assertEquals("王经理", user.getRealName());
        assertEquals("MANAGER", user.getRole());

        System.out.println("✅ 登录成功测试通过，用户: " + user.getRealName());
    }

    @Test
    @DisplayName("登录失败 - 错误的密码")
    void testLogin_WrongPassword() {
        // When & Then: 使用错误密码应该抛出异常
        assertThrows(BusinessException.class, () -> {
            userService.login("admin", "wrongpassword");
        }, "错误密码应该抛出BusinessException");

        System.out.println("✅ 密码错误测试通过");
    }

    @Test
    @DisplayName("登录失败 - 用户不存在")
    void testLogin_UserNotFound() {
        // When & Then: 使用不存在的用户名应该抛出异常
        assertThrows(BusinessException.class, () -> {
            userService.login("nonexistent", "123456");
        }, "不存在的用户应该抛出BusinessException");

        System.out.println("✅ 用户不存在测试通过");
    }

    @Test
    @DisplayName("获取用户列表 - 应返回所有用户")
    void testGetAllUsers() {
        // When: 获取用户列表
        List<User> users = userService.listAll();

        // Then: 应该返回种子数据中的5个用户
        assertNotNull(users);
        assertTrue(users.size() >= 5, "应该至少有5个测试用户");

        System.out.println("✅ 用户列表测试通过，共 " + users.size() + " 个用户");
    }

    @Test
    @DisplayName("根据ID获取用户")
    void testGetUserById() {
        // When: 获取admin用户
        User user = userMapper.selectById(1L);

        // Then: 应该返回正确的用户信息
        assertNotNull(user);
        assertEquals("admin", user.getUsername());
        assertEquals("MANAGER", user.getRole());

        System.out.println("✅ 根据ID获取用户测试通过");
    }
}
