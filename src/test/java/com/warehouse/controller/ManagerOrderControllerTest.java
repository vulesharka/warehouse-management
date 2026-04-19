package com.warehouse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.warehouse.dto.request.DeclineRequest;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ManagerOrderControllerTest {

    @Mock OrderService orderService;
    @InjectMocks ManagerOrderController managerOrderController;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private OrderResponse orderResponse;
    private OrderSummaryResponse orderSummary;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(managerOrderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        orderResponse = new OrderResponse(1L, "ORD-TEST", OrderStatus.AWAITING_APPROVAL,
                LocalDateTime.now(), LocalDateTime.now(), "client", null, List.of());
        orderSummary = new OrderSummaryResponse(1L, "ORD-TEST", OrderStatus.AWAITING_APPROVAL,
                LocalDateTime.now(), LocalDateTime.now(), "client");
    }

    @Test
    void getAllOrders_returns200WithList() throws Exception {
        when(orderService.getAllOrders(null)).thenReturn(List.of(orderSummary));

        mockMvc.perform(get("/api/manager/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-TEST"))
                .andExpect(jsonPath("$[0].status").value("AWAITING_APPROVAL"));
    }

    @Test
    void getAllOrders_returns200FilteredByStatus() throws Exception {
        when(orderService.getAllOrders(OrderStatus.AWAITING_APPROVAL)).thenReturn(List.of(orderSummary));

        mockMvc.perform(get("/api/manager/orders").param("status", "AWAITING_APPROVAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("AWAITING_APPROVAL"));
    }

    @Test
    void getOrderDetail_returns200WhenFound() throws Exception {
        when(orderService.getOrderDetail(1L)).thenReturn(orderResponse);

        mockMvc.perform(get("/api/manager/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"));
    }

    @Test
    void getOrderDetail_returns404WhenNotFound() throws Exception {
        when(orderService.getOrderDetail(99L))
                .thenThrow(new ResourceNotFoundException("Order", 99L));

        mockMvc.perform(get("/api/manager/orders/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void approveOrder_returns200() throws Exception {
        OrderResponse approved = new OrderResponse(1L, "ORD-TEST", OrderStatus.APPROVED,
                LocalDateTime.now(), LocalDateTime.now(), "client", null, List.of());
        when(orderService.approveOrder(1L)).thenReturn(approved);

        mockMvc.perform(post("/api/manager/orders/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approveOrder_returns400OnInvalidTransition() throws Exception {
        when(orderService.approveOrder(1L))
                .thenThrow(new InvalidStatusTransitionException(OrderStatus.CREATED, OrderStatus.APPROVED));

        mockMvc.perform(post("/api/manager/orders/1/approve"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approveOrder_returns404WhenNotFound() throws Exception {
        when(orderService.approveOrder(99L))
                .thenThrow(new ResourceNotFoundException("Order", 99L));

        mockMvc.perform(post("/api/manager/orders/99/approve"))
                .andExpect(status().isNotFound());
    }

    @Test
    void declineOrder_returns200WithReason() throws Exception {
        OrderResponse declined = new OrderResponse(1L, "ORD-TEST", OrderStatus.DECLINED,
                null, LocalDateTime.now(), "client", "Out of stock", List.of());
        when(orderService.declineOrder(eq(1L), eq("Out of stock"))).thenReturn(declined);

        mockMvc.perform(post("/api/manager/orders/1/decline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeclineRequest("Out of stock"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.declineReason").value("Out of stock"));
    }

    @Test
    void declineOrder_returns200WithoutReason() throws Exception {
        OrderResponse declined = new OrderResponse(1L, "ORD-TEST", OrderStatus.DECLINED,
                null, LocalDateTime.now(), "client", null, List.of());
        when(orderService.declineOrder(eq(1L), isNull())).thenReturn(declined);

        mockMvc.perform(post("/api/manager/orders/1/decline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));
    }

    @Test
    void declineOrder_returns404WhenNotFound() throws Exception {
        when(orderService.declineOrder(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Order", 99L));

        mockMvc.perform(post("/api/manager/orders/99/decline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeclineRequest("reason"))))
                .andExpect(status().isNotFound());
    }
}
