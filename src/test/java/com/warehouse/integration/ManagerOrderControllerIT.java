package com.warehouse.integration;

import com.warehouse.dto.request.DeclineRequest;
import com.warehouse.entity.Order;
import com.warehouse.entity.User;
import com.warehouse.enums.OrderStatus;
import com.warehouse.repository.OrderRepository;
import com.warehouse.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ManagerOrderControllerIT extends BaseIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    private User client;

    @BeforeEach
    void setUp() {
        client = userRepository.findByUsername("client").orElseThrow();
    }

    private Order saveOrder(String orderNumber, OrderStatus status) {
        return orderRepository.save(Order.builder()
                .orderNumber(orderNumber)
                .status(status)
                .client(client)
                .submittedDate(status == OrderStatus.AWAITING_APPROVAL ? LocalDateTime.now() : null)
                .build());
    }

    @Test
    void getAllOrders_returns200_whenAuthenticatedAsManager() throws Exception {
        mockMvc.perform(get("/api/manager/orders")
                        .header("Authorization", managerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getAllOrders_returns403_whenAuthenticatedAsClient() throws Exception {
        mockMvc.perform(get("/api/manager/orders")
                        .header("Authorization", clientToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllOrders_returns403_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/manager/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllOrders_returns200_filteredByStatus() throws Exception {
        saveOrder("ORD-IT-MGR-001", OrderStatus.AWAITING_APPROVAL);

        mockMvc.perform(get("/api/manager/orders")
                        .param("status", "AWAITING_APPROVAL")
                        .header("Authorization", managerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("AWAITING_APPROVAL"));
    }

    @Test
    void getOrderDetail_returns200_whenOrderExists() throws Exception {
        Order order = saveOrder("ORD-IT-MGR-002", OrderStatus.AWAITING_APPROVAL);

        mockMvc.perform(get("/api/manager/orders/" + order.getId())
                        .header("Authorization", managerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-IT-MGR-002"))
                .andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"));
    }

    @Test
    void getOrderDetail_returns404_whenOrderNotFound() throws Exception {
        mockMvc.perform(get("/api/manager/orders/999999")
                        .header("Authorization", managerToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void approveOrder_returns200_whenAwaitingApproval() throws Exception {
        Order order = saveOrder("ORD-IT-MGR-003", OrderStatus.AWAITING_APPROVAL);

        mockMvc.perform(post("/api/manager/orders/" + order.getId() + "/approve")
                        .header("Authorization", managerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approveOrder_returns400_whenStatusIsCreated() throws Exception {
        Order order = saveOrder("ORD-IT-MGR-004", OrderStatus.CREATED);

        mockMvc.perform(post("/api/manager/orders/" + order.getId() + "/approve")
                        .header("Authorization", managerToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approveOrder_returns404_whenOrderNotFound() throws Exception {
        mockMvc.perform(post("/api/manager/orders/999999/approve")
                        .header("Authorization", managerToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void declineOrder_returns200_withReason() throws Exception {
        Order order = saveOrder("ORD-IT-MGR-005", OrderStatus.AWAITING_APPROVAL);

        mockMvc.perform(post("/api/manager/orders/" + order.getId() + "/decline")
                        .header("Authorization", managerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeclineRequest("Out of stock"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.declineReason").value("Out of stock"));
    }

    @Test
    void declineOrder_returns200_withoutReason() throws Exception {
        Order order = saveOrder("ORD-IT-MGR-006", OrderStatus.AWAITING_APPROVAL);

        mockMvc.perform(post("/api/manager/orders/" + order.getId() + "/decline")
                        .header("Authorization", managerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));
    }

    @Test
    void declineOrder_returns403_whenAuthenticatedAsClient() throws Exception {
        Order order = saveOrder("ORD-IT-MGR-007", OrderStatus.AWAITING_APPROVAL);

        mockMvc.perform(post("/api/manager/orders/" + order.getId() + "/decline")
                        .header("Authorization", clientToken()))
                .andExpect(status().isForbidden());
    }
}
