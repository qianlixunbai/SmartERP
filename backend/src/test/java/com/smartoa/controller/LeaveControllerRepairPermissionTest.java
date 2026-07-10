package com.smartoa.controller;

import com.smartoa.common.BusinessException;
import com.smartoa.entity.User;
import com.smartoa.service.LeaveService;
import com.smartoa.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveController repair 接口权限测试（Mockito）")
class LeaveControllerRepairPermissionTest {

    @Mock
    private LeaveService leaveService;

    @Mock
    private UserService userService;

    @InjectMocks
    private LeaveController leaveController;

    @Nested
    @DisplayName("repair 权限验证")
    class RepairPermissionTests {

        @Test
        @DisplayName("未登录用户应返回 401")
        void testNotLoggedIn_ShouldReturn401() {
            when(userService.getLoginUser()).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> leaveController.repairStuckRequests());

            assertEquals(401, exception.getCode());
            assertEquals("请先登录", exception.getMessage());
            verify(leaveService, never()).repairStuckRequests();
        }

        @Test
        @DisplayName("EMPLOYEE 角色应返回 403")
        void testEmployee_ShouldReturn403() {
            User employee = new User();
            employee.setId(1L);
            employee.setRole("EMPLOYEE");
            when(userService.getLoginUser()).thenReturn(employee);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> leaveController.repairStuckRequests());

            assertEquals(403, exception.getCode());
            assertEquals("无权限", exception.getMessage());
            verify(leaveService, never()).repairStuckRequests();
        }

        @Test
        @DisplayName("MANAGER 角色应能执行 repair")
        void testManager_ShouldExecuteRepair() {
            User manager = new User();
            manager.setId(1L);
            manager.setRole("MANAGER");
            when(userService.getLoginUser()).thenReturn(manager);
            when(leaveService.repairStuckRequests()).thenReturn(5);

            var result = leaveController.repairStuckRequests();

            assertEquals(5, result.getData());
            verify(leaveService, times(1)).repairStuckRequests();
        }
    }
}
