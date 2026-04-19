package com.warehouse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.warehouse.dto.request.OrderItemRequest;
import com.warehouse.dto.request.OrderRequest;
import com.warehouse.dto.response.OrderResponse;
import com.warehouse.dto.response.OrderSummaryResponse;
import com.warehouse.enums.OrderStatus;
import com.warehouse.exception.GlobalExceptionHandler;
import com.warehouse.exception.InvalidStatusTransitionException;
import com.warehouse.exception.ResourceNotFoundException;
import com.warehouse.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock OrderService orderService;
    @InjectMocks OrderController orderController;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private OrderResponse orderResponse;
    private OrderSummaryResponse orderSummary;
    private OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver(), new PageableHandlerMethodArgumentResolver())
                .build();
        orderResponse = new OrderResponse(1L, "ORD-TEST", OrderStatus.CREATED,
                null, LocalDateTime.now(), "client", null, List.of());
        orderSummary = new OrderSummaryResponse(1L, "ORD-TEST", OrderStatus.CREATED,
                null, LocalDateTime.now(), "client");
        orderRequest = new OrderRequest(
                List.of(new OrderItemRequest(1L, 3, LocalDate.now().plusDays(7))));

        User principal = new User("client", "", List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @org.junit.jupiter.api.AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createOrder_returns201() throws Exception {
        when(orderService.createOrder(any(), eq("client"))).thenReturn(orderResponse);

        mockMvc.perform(post("/api/orders")
.contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("ORD-TEST"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void createOrder_returns400WhenBodyInvalid() throws Exception {
        mockMvc.perform(post("/api/orders")
.contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyOrders_returns200WithList() throws Exception {
        when(orderService.getClientOrders(eq("client"), isNull(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(orderSummary)));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value("ORD-TEST"));
    }

    @Test
    void getMyOrders_returns200FilteredByStatus() throws Exception {
        when(orderService.getClientOrders(eq("client"), eq(OrderStatus.CREATED), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(orderSummary)));

        mockMvc.perform(get("/api/orders").param("status", "CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("CREATED"));
    }

    @Test
    void getMyOrder_returns200WhenFound() throws Exception {
        when(orderService.getClientOrderDetail(eq(1L), eq("client"))).thenReturn(orderResponse);

        mockMvc.perform(get("/api/orders/1")
)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getMyOrder_returns404WhenNotFound() throws Exception {
        when(orderService.getClientOrderDetail(eq(99L), eq("client")))
                .thenThrow(new ResourceNotFoundException("Order", 99L));

        mockMvc.perform(get("/api/orders/99")
)
                .andExpect(status().isNotFound());
    }

    @Test
    void updateOrder_returns200() throws Exception {
        when(orderService.updateOrder(eq(1L), any(), eq("client"))).thenReturn(orderResponse);

        mockMvc.perform(put("/api/orders/1")
.contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void updateOrder_returns404WhenNotFound() throws Exception {
        when(orderService.updateOrder(eq(99L), any(), eq("client")))
                .thenThrow(new ResourceNotFoundException("Order", 99L));

        mockMvc.perform(put("/api/orders/99")
.contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void submitOrder_returns200() throws Exception {
        OrderResponse submitted = new OrderResponse(1L, "ORD-TEST", OrderStatus.AWAITING_APPROVAL,
                LocalDateTime.now(), LocalDateTime.now(), "client", null, List.of());
        when(orderService.submitOrder(eq(1L), eq("client"))).thenReturn(submitted);

        mockMvc.perform(post("/api/orders/1/submit")
)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"));
    }

    @Test
    void submitOrder_returns400OnInvalidTransition() throws Exception {
        when(orderService.submitOrder(eq(1L), eq("client")))
                .thenThrow(new InvalidStatusTransitionException(OrderStatus.CANCELED, OrderStatus.AWAITING_APPROVAL));

        mockMvc.perform(post("/api/orders/1/submit")
)
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelOrder_returns200() throws Exception {
        OrderResponse canceled = new OrderResponse(1L, "ORD-TEST", OrderStatus.CANCELED,
                null, LocalDateTime.now(), "client", null, List.of());
        when(orderService.cancelOrder(eq(1L), eq("client"))).thenReturn(canceled);

        mockMvc.perform(post("/api/orders/1/cancel")
)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void cancelOrder_returns400OnInvalidTransition() throws Exception {
        when(orderService.cancelOrder(eq(1L), eq("client")))
                .thenThrow(new InvalidStatusTransitionException(OrderStatus.FULFILLED, OrderStatus.CANCELED));

        mockMvc.perform(post("/api/orders/1/cancel")
)
                .andExpect(status().isBadRequest());
    }
}
