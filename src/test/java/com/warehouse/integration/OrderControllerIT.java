package com.warehouse.integration;

import com.warehouse.dto.request.OrderItemRequest;
import com.warehouse.dto.request.OrderRequest;
import com.warehouse.entity.InventoryItem;
import com.warehouse.entity.Order;
import com.warehouse.entity.User;
import com.warehouse.enums.OrderStatus;
import com.warehouse.repository.InventoryItemRepository;
import com.warehouse.repository.OrderRepository;
import com.warehouse.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderControllerIT extends BaseIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    private User client;
    private InventoryItem item;

    @BeforeEach
    void setUp() {
        client = userRepository.findByUsername("client").orElseThrow();
        item = inventoryItemRepository.save(InventoryItem.builder()
                .name("Test Widget")
                .quantity(100)
                .unitPrice(new BigDecimal("9.99"))
                .packageVolume(new BigDecimal("0.5"))
                .build());
    }

    private Order saveOrder(String orderNumber, OrderStatus status) {
        return orderRepository.save(Order.builder()
                .orderNumber(orderNumber)
                .status(status)
                .client(client)
                .submittedDate(status == OrderStatus.AWAITING_APPROVAL ? LocalDateTime.now() : null)
                .build());
    }

    private OrderRequest buildOrderRequest() {
        return new OrderRequest(List.of(
                new OrderItemRequest(item.getId(), 2, LocalDate.now().plusDays(7))
        ));
    }

    @Test
    void createOrder_returns201_whenClientAuthenticated() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", clientToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildOrderRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.clientUsername").value("client"));
    }

    @Test
    void createOrder_returns403_whenAuthenticatedAsManager() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", managerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildOrderRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createOrder_returns400_whenBodyIsEmpty() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", clientToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_returns403_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildOrderRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyOrders_returns200_withPaginatedList() throws Exception {
        saveOrder("ORD-IT-CLI-001", OrderStatus.CREATED);

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", clientToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getMyOrder_returns200_whenOrderBelongsToClient() throws Exception {
        Order order = saveOrder("ORD-IT-CLI-002", OrderStatus.CREATED);

        mockMvc.perform(get("/api/orders/" + order.getId())
                        .header("Authorization", clientToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-IT-CLI-002"));
    }

    @Test
    void getMyOrder_returns404_whenOrderNotFound() throws Exception {
        mockMvc.perform(get("/api/orders/999999")
                        .header("Authorization", clientToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void submitOrder_returns200_changesStatusToAwaitingApproval() throws Exception {
        Order order = saveOrder("ORD-IT-CLI-003", OrderStatus.CREATED);

        mockMvc.perform(post("/api/orders/" + order.getId() + "/submit")
                        .header("Authorization", clientToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"));
    }

    @Test
    void submitOrder_returns400_whenAlreadySubmitted() throws Exception {
        Order order = saveOrder("ORD-IT-CLI-004", OrderStatus.AWAITING_APPROVAL);

        mockMvc.perform(post("/api/orders/" + order.getId() + "/submit")
                        .header("Authorization", clientToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelOrder_returns200_changesStatusToCanceled() throws Exception {
        Order order = saveOrder("ORD-IT-CLI-005", OrderStatus.CREATED);

        mockMvc.perform(post("/api/orders/" + order.getId() + "/cancel")
                        .header("Authorization", clientToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void cancelOrder_returns400_whenAlreadyFulfilled() throws Exception {
        Order order = saveOrder("ORD-IT-CLI-006", OrderStatus.FULFILLED);

        mockMvc.perform(post("/api/orders/" + order.getId() + "/cancel")
                        .header("Authorization", clientToken()))
                .andExpect(status().isBadRequest());
    }
}
