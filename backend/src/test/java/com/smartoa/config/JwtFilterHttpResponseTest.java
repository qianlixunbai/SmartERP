package com.smartoa.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartoa.entity.User;
import com.smartoa.mapper.UserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtFilter HTTP 响应测试（无 Spring Context）")
class JwtFilterHttpResponseTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserMapper userMapper;

    @Mock
    private FilterChain filterChain;

    private JwtFilter jwtFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtFilter(jwtUtil, userMapper, objectMapper);
    }

    @Test
    @DisplayName("无 Authorization header → HTTP 401, body.code=401, FilterChain 未执行")
    void noAuthHeader_ShouldReturn401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/leave/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());

        var body = objectMapper.readTree(response.getContentAsString());
        assertEquals(401, body.get("code").asInt());
        assertEquals("未登录", body.get("message").asText());
        assertTrue(body.get("data").isNull());

        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("Token 无效 → HTTP 401, body.code=401, FilterChain 未执行")
    void invalidToken_ShouldReturn401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/leave/1");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        var body = objectMapper.readTree(response.getContentAsString());
        assertEquals(401, body.get("code").asInt());
        assertEquals("token无效或已过期", body.get("message").asText());

        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("用户不存在 → HTTP 401, body.code=401, FilterChain 未执行")
    void userNotFound_ShouldReturn401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/leave/1");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("valid-token")).thenReturn(999L);
        when(userMapper.selectById(999L)).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        var body = objectMapper.readTree(response.getContentAsString());
        assertEquals(401, body.get("code").asInt());
        assertEquals("用户不存在", body.get("message").asText());

        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("合法 Token → FilterChain 执行, userId 可用, finally 清理上下文")
    void validToken_ShouldProceedAndCleanup() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/leave/1");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = new User();
        user.setId(42L);

        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("valid-token")).thenReturn(42L);
        when(userMapper.selectById(42L)).thenReturn(user);

        // filterChain 执行时验证 userId 可用
        doAnswer(invocation -> {
            assertEquals(42L, UserContextHolder.getUserId());
            return null;
        }).when(filterChain).doFilter(any(), any());

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        // finally 清理后 userId 应为 null
        assertNull(UserContextHolder.getUserId());
    }

    @Test
    @DisplayName("/api/login 路径应跳过 JWT 检查")
    void loginPath_ShouldSkipJwtCheck() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
    }
}
