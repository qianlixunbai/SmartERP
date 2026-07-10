package com.smartoa.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("GlobalExceptionHandler HTTP 状态码测试（MockMvc standalone）")
class GlobalExceptionHandlerHttpStatusTest {

    /**
     * 仅用于测试的内部 Controller，触发各种异常
     */
    @RestController
    static class TestController {
        @GetMapping("/test/business-400")
        public void throw400() { throw new BusinessException(400, "请求参数错误"); }

        @GetMapping("/test/business-401")
        public void throw401() { throw new BusinessException(401, "请先登录"); }

        @GetMapping("/test/business-403")
        public void throw403() { throw new BusinessException(403, "无权限"); }

        @GetMapping("/test/business-404")
        public void throw404() { throw new BusinessException(404, "资源不存在"); }

        @GetMapping("/test/business-500")
        public void throw500() { throw new BusinessException(500, "服务器内部错误"); }

        @GetMapping("/test/business-1002")
        public void throw1002() { throw new BusinessException(1002, "用户名或密码错误"); }

        @GetMapping("/test/business-default")
        public void throwDefault() { throw new BusinessException("业务校验失败"); }

        @GetMapping("/test/runtime-exception")
        public void throwRuntime() { throw new RuntimeException("未预期异常"); }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("BusinessException(400) → HTTP 400, body.code=400")
    void testBusiness400() throws Exception {
        mockMvc.perform(get("/test/business-400"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请求参数错误"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("BusinessException(401) → HTTP 401, body.code=401")
    void testBusiness401() throws Exception {
        mockMvc.perform(get("/test/business-401"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请先登录"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("BusinessException(403) → HTTP 403, body.code=403")
    void testBusiness403() throws Exception {
        mockMvc.perform(get("/test/business-403"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("BusinessException(404) → HTTP 404, body.code=404")
    void testBusiness404() throws Exception {
        mockMvc.perform(get("/test/business-404"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("资源不存在"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("BusinessException(500) → HTTP 422, body.code=500（业务校验失败）")
    void testBusiness500() throws Exception {
        mockMvc.perform(get("/test/business-500"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("服务器内部错误"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("BusinessException(1002) → HTTP 401, body.code=1002")
    void testBusiness1002() throws Exception {
        mockMvc.perform(get("/test/business-1002"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(1002))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("BusinessException(无显式 code) → HTTP 422, body.code=500")
    void testBusinessDefault() throws Exception {
        mockMvc.perform(get("/test/business-default"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("业务校验失败"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("RuntimeException → HTTP 500, body.code=500, 不泄露堆栈")
    void testRuntimeException() throws Exception {
        mockMvc.perform(get("/test/runtime-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("服务器内部错误"))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
