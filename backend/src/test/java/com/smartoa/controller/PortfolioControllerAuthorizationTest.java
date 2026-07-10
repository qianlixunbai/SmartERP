package com.smartoa.controller;

import com.smartoa.common.GlobalExceptionHandler;
import com.smartoa.entity.*;
import com.smartoa.service.PortfolioService;
import com.smartoa.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioController 组织级权限测试")
class PortfolioControllerAuthorizationTest {

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PortfolioController(portfolioService, userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @ParameterizedTest(name = "null user: {0}")
    @MethodSource("endpoints")
    void nullUser_ShouldReturn401AndNotCallService(EndpointSpec endpoint) throws Exception {
        reset(portfolioService);
        when(userService.getLoginUser()).thenReturn(null);

        perform(endpoint)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请先登录"))
                .andExpect(jsonPath("$.data").value(nullValue()));

        verifyNoInteractions(portfolioService);
    }

    @ParameterizedTest(name = "manager without id: {0}")
    @MethodSource("endpoints")
    void managerWithoutId_ShouldReturn401AndNotCallService(EndpointSpec endpoint) throws Exception {
        reset(portfolioService);
        User manager = user(null, "MANAGER");
        when(userService.getLoginUser()).thenReturn(manager);

        perform(endpoint)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请先登录"))
                .andExpect(jsonPath("$.data").value(nullValue()));

        verifyNoInteractions(portfolioService);
    }

    @ParameterizedTest(name = "employee: {0}")
    @MethodSource("endpoints")
    void employee_ShouldReturn403AndNotCallService(EndpointSpec endpoint) throws Exception {
        reset(portfolioService);
        when(userService.getLoginUser()).thenReturn(user(10L, "EMPLOYEE"));

        perform(endpoint)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限"))
                .andExpect(jsonPath("$.data").value(nullValue()));

        verifyNoInteractions(portfolioService);
    }

    @ParameterizedTest(name = "manager: {0}")
    @MethodSource("endpoints")
    void manager_ShouldReturn200AndRouteToExpectedService(EndpointSpec endpoint) throws Exception {
        reset(portfolioService);
        stubServiceResponse(endpoint);
        when(userService.getLoginUser()).thenReturn(user(1L, "MANAGER"));

        perform(endpoint).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));

        endpoint.verifyService().accept(portfolioService);
        verifyNoMoreInteractions(portfolioService);
    }

    private void stubServiceResponse(EndpointSpec endpoint) {
        switch (endpoint.name()) {
            case "GET portfolios" -> when(portfolioService.getPortfolios()).thenReturn(List.of());
            case "POST portfolios" -> when(portfolioService.createPortfolio(any(), any(), any()))
                    .thenReturn(new Portfolio());
            case "GET assets" -> when(portfolioService.getAssets()).thenReturn(List.of());
            case "POST assets" -> when(portfolioService.createAsset(any(), any(), any(), any()))
                    .thenReturn(new PortfolioAsset());
            case "PUT asset price" -> doNothing().when(portfolioService)
                    .updateAssetPrice(anyLong(), any());
            case "GET holdings" -> when(portfolioService.getHoldingsSummary(1L)).thenReturn(List.of());
            case "GET trades" -> when(portfolioService.getTrades(1L)).thenReturn(List.of());
            case "POST trades" -> when(portfolioService.executeTrade(any(), eq(1L)))
                    .thenReturn(new PortfolioTrade());
            case "GET dividends" -> when(portfolioService.getDividends(1L)).thenReturn(List.of());
            case "POST dividends" -> when(portfolioService.recordDividend(any()))
                    .thenReturn(new PortfolioDividend());
            case "GET allocation" -> when(portfolioService.getAssetAllocation(1L)).thenReturn(List.of());
            case "GET performance" -> when(portfolioService.getPortfolioPerformance(1L)).thenReturn(Map.of());
            case "GET dashboard" -> when(portfolioService.getPortfolioDashboard(1L)).thenReturn(Map.of());
            default -> throw new IllegalArgumentException("Unknown endpoint: " + endpoint.name());
        }
    }

    private org.springframework.test.web.servlet.ResultActions perform(EndpointSpec endpoint)
            throws Exception {
        return mockMvc.perform(endpoint.request().get());
    }

    private static User user(Long id, String role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private static Stream<EndpointSpec> endpoints() {
        return Stream.of(
                new EndpointSpec("GET portfolios", () -> get("/api/portfolio/portfolios"),
                        service -> verify(service).getPortfolios()),
                new EndpointSpec("POST portfolios", () -> post("/api/portfolio/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"description\":\"Test\",\"baseCurrency\":\"USD\"}"),
                        service -> verify(service).createPortfolio("Test", "Test", "USD")),
                new EndpointSpec("GET assets", () -> get("/api/portfolio/assets"),
                        service -> verify(service).getAssets()),
                new EndpointSpec("POST assets", () -> post("/api/portfolio/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"AAPL\",\"name\":\"Apple\",\"assetType\":\"STOCK\",\"currency\":\"USD\"}"),
                        service -> verify(service).createAsset("AAPL", "Apple", "STOCK", "USD")),
                new EndpointSpec("PUT asset price", () -> put("/api/portfolio/assets/1/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPrice\":100.0000}"),
                        service -> verify(service).updateAssetPrice(eq(1L), any())),
                new EndpointSpec("GET holdings", () -> get("/api/portfolio/holdings").param("portfolioId", "1"),
                        service -> verify(service).getHoldingsSummary(1L)),
                new EndpointSpec("GET trades", () -> get("/api/portfolio/trades").param("portfolioId", "1"),
                        service -> verify(service).getTrades(1L)),
                new EndpointSpec("POST trades", () -> post("/api/portfolio/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"portfolioId\":1,\"assetId\":1,\"tradeType\":\"BUY\",\"quantity\":10,\"price\":100}"),
                        service -> verify(service).executeTrade(any(), eq(1L))),
                new EndpointSpec("GET dividends", () -> get("/api/portfolio/dividends").param("portfolioId", "1"),
                        service -> verify(service).getDividends(1L)),
                new EndpointSpec("POST dividends", () -> post("/api/portfolio/dividends")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"portfolioId\":1,\"assetId\":1,\"amount\":100,\"perShare\":1.0,\"quantity\":10,\"dividendDate\":\"2026-06-30\"}"),
                        service -> verify(service).recordDividend(any())),
                new EndpointSpec("GET allocation", () -> get("/api/portfolio/allocation").param("portfolioId", "1"),
                        service -> verify(service).getAssetAllocation(1L)),
                new EndpointSpec("GET performance", () -> get("/api/portfolio/performance").param("portfolioId", "1"),
                        service -> verify(service).getPortfolioPerformance(1L)),
                new EndpointSpec("GET dashboard", () -> get("/api/portfolio/dashboard").param("portfolioId", "1"),
                        service -> verify(service).getPortfolioDashboard(1L))
        );
    }

    private record EndpointSpec(
            String name,
            Supplier<MockHttpServletRequestBuilder> request,
            Consumer<PortfolioService> verifyService) {

        @Override
        public String toString() {
            return name;
        }
    }
}
