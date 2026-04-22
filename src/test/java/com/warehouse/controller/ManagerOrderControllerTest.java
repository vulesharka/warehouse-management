package com.warehouse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.warehouse.dto.request.DeclineRequest;
import com.warehouse.dto.request.ScheduleDeliveryRequest;
import com.warehouse.dto.response.AvailableDaysResponse;
import com.warehouse.dto.response.DeliveryResponse;
import com.warehouse.dto.response.OrderResponse;
import com.warehouse.dto.response.OrderSummaryResponse;
import com.warehouse.dto.response.TruckResponse;
import com.warehouse.enums.OrderStatus;
import com.warehouse.exception.BusinessException;
import com.warehouse.exception.GlobalExceptionHandler;
import com.warehouse.exception.InvalidStatusTransitionException;
import com.warehouse.exception.ResourceNotFoundException;
import com.warehouse.service.DeliveryService;
import com.warehouse.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ManagerOrderControllerTest {

    @Mock OrderService orderService;
    @Mock DeliveryService deliveryService;
    @InjectMocks ManagerOrderController managerOrderController;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private OrderResponse orderResponse;
    private OrderSummaryResponse orderSummary;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(managerOrderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        orderResponse = new OrderResponse(1L, "ORD-TEST", OrderStatus.AWAITING_APPROVAL,
                LocalDateTime.now(), LocalDateTime.now(), "client", null, List.of());
        orderSummary = new OrderSummaryResponse(1L, "ORD-TEST", OrderStatus.AWAITING_APPROVAL,
                LocalDateTime.now(), LocalDateTime.now(), "client");
    }

    @Test
    void getAllOrders_returns200WithList() throws Exception {
        when(orderService.getAllOrders(isNull(), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(orderSummary)));

        mockMvc.perform(get("/api/manager/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value("ORD-TEST"))
                .andExpect(jsonPath("$.content[0].status").value("AWAITING_APPROVAL"));
    }

    @Test
    void getAllOrders_returns200FilteredByStatus() throws Exception {
        when(orderService.getAllOrders(eq(OrderStatus.AWAITING_APPROVAL), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(orderSummary)));

        mockMvc.perform(get("/api/manager/orders").param("status", "AWAITING_APPROVAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("AWAITING_APPROVAL"));
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

    @Test
    void scheduleDelivery_returns201OnSuccess() throws Exception {
        LocalDate deliveryDate = LocalDate.now().plusDays(3);
        ScheduleDeliveryRequest request = new ScheduleDeliveryRequest(deliveryDate, List.of(1L));
        TruckResponse truck = new TruckResponse(1L, "CH-001", "AB-1234", new BigDecimal("20.0"));
        DeliveryResponse delivery = new DeliveryResponse(1L, 1L, "ORD-TEST", deliveryDate, List.of(truck));

        when(deliveryService.scheduleDelivery(eq(1L), any())).thenReturn(delivery);

        mockMvc.perform(post("/api/manager/orders/1/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("ORD-TEST"))
                .andExpect(jsonPath("$.trucks[0].licensePlate").value("AB-1234"));
    }

    @Test
    void scheduleDelivery_returns400WhenOrderNotApproved() throws Exception {
        LocalDate deliveryDate = LocalDate.now().plusDays(3);
        ScheduleDeliveryRequest request = new ScheduleDeliveryRequest(deliveryDate, List.of(1L));

        when(deliveryService.scheduleDelivery(eq(1L), any()))
                .thenThrow(new InvalidStatusTransitionException(OrderStatus.AWAITING_APPROVAL, OrderStatus.UNDER_DELIVERY));

        mockMvc.perform(post("/api/manager/orders/1/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void scheduleDelivery_returns404WhenOrderNotFound() throws Exception {
        LocalDate deliveryDate = LocalDate.now().plusDays(3);
        ScheduleDeliveryRequest request = new ScheduleDeliveryRequest(deliveryDate, List.of(1L));

        when(deliveryService.scheduleDelivery(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Order", 99L));

        mockMvc.perform(post("/api/manager/orders/99/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void scheduleDelivery_returns400WhenInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/manager/orders/1/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAvailableDays_returns200WithDays() throws Exception {
        List<LocalDate> days = List.of(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
        when(deliveryService.getAvailableDays(eq(1L), isNull()))
                .thenReturn(new AvailableDaysResponse(days));

        mockMvc.perform(get("/api/manager/orders/1/available-days"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableDays").isArray())
                .andExpect(jsonPath("$.availableDays.length()").value(2));
    }

    @Test
    void getAvailableDays_returns200WithDaysParam() throws Exception {
        List<LocalDate> days = List.of(LocalDate.now().plusDays(1));
        when(deliveryService.getAvailableDays(eq(1L), eq(7)))
                .thenReturn(new AvailableDaysResponse(days));

        mockMvc.perform(get("/api/manager/orders/1/available-days").param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableDays.length()").value(1));
    }

    @Test
    void getAvailableDays_returns400WhenOrderNotApproved() throws Exception {
        when(deliveryService.getAvailableDays(eq(1L), any()))
                .thenThrow(new BusinessException("Available days can only be queried for APPROVED orders"));

        mockMvc.perform(get("/api/manager/orders/1/available-days"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAvailableDays_returns404WhenOrderNotFound() throws Exception {
        when(deliveryService.getAvailableDays(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Order", 99L));

        mockMvc.perform(get("/api/manager/orders/99/available-days"))
                .andExpect(status().isNotFound());
    }
}
