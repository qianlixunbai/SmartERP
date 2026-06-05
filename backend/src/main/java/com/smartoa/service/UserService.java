package com.smartoa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartoa.common.BusinessException;
import com.smartoa.config.UserContextHolder;
import com.smartoa.entity.User;
import com.smartoa.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public User login(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException(1002, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(1002, "用户名或密码错误");
        }
        return user;
    }

    public User getLoginUser() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return null;
        }
        return userMapper.selectById(userId);
    }

    public List<User> listAll() {
        return userMapper.selectList(null);
    }
}
