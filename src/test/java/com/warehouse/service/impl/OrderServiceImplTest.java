package com.warehouse.service.impl;

import com.warehouse.dto.request.OrderItemRequest;
import com.warehouse.dto.request.OrderRequest;
import com.warehouse.dto.response.OrderResponse;
import com.warehouse.entity.InventoryItem;
import com.warehouse.entity.Order;
import com.warehouse.entity.User;
import com.warehouse.enums.OrderStatus;
import com.warehouse.enums.Role;
import com.warehouse.exception.InvalidStatusTransitionException;
import com.warehouse.mapper.OrderMapper;
import com.warehouse.repository.OrderRepository;
import com.warehouse.repository.UserRepository;
import com.warehouse.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock UserRepository userRepository;
    @Mock InventoryService inventoryService;
    @Mock OrderMapper orderMapper;

    @InjectMocks OrderServiceImpl orderService;

    private User client;
    private InventoryItem item;

    @BeforeEach
    void setUp() {
        client = User.builder().id(1L).username("client")
                .email("c@test.com").role(Role.CLIENT).build();
        item = InventoryItem.builder().id(1L).name("Widget")
                .quantity(100).unitPrice(BigDecimal.ONE)
                .packageVolume(new BigDecimal("0.5")).build();
    }

    @Test
    void createOrder_returnsOrderWithCreatedStatus() {
        OrderRequest request = new OrderRequest(List.of(
                new OrderItemRequest(1L, 5, LocalDate.now().plusDays(7))));
        Order savedOrder = buildOrder(OrderStatus.CREATED);
        OrderResponse expectedResponse = new OrderResponse(
                1L, "ORD-TEST", OrderStatus.CREATED, null,
                LocalDateTime.now(), "client", null, List.of());

        when(userRepository.findByUsername("client")).thenReturn(Optional.of(client));
        when(inventoryService.findById(1L)).thenReturn(item);
        when(orderRepository.save(any())).thenReturn(savedOrder);
        when(orderMapper.toResponse(savedOrder)).thenReturn(expectedResponse);

        OrderResponse response = orderService.createOrder(request, "client");

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void submitOrder_changesStatusToAwaitingApproval() {
        Order order = buildOrder(OrderStatus.CREATED);
        OrderResponse expectedResponse = new OrderResponse(
                1L, "ORD-TEST", OrderStatus.AWAITING_APPROVAL, LocalDateTime.now(),
                LocalDateTime.now(), "client", null, List.of());

        when(userRepository.findByUsername("client")).thenReturn(Optional.of(client));
        when(orderRepository.findByIdAndClient(1L, client)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(expectedResponse);

        OrderResponse response = orderService.submitOrder(1L, "client");

        assertThat(response.getStatus()).isEqualTo(OrderStatus.AWAITING_APPROVAL);
    }

    @Test
    void cancelOrder_changesStatusToCanceled() {
        Order order = buildOrder(OrderStatus.AWAITING_APPROVAL);
        OrderResponse expectedResponse = new OrderResponse(
                1L, "ORD-TEST", OrderStatus.CANCELED, null,
                LocalDateTime.now(), "client", null, List.of());

        when(userRepository.findByUsername("client")).thenReturn(Optional.of(client));
        when(orderRepository.findByIdAndClient(1L, client)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(expectedResponse);

        OrderResponse response = orderService.cancelOrder(1L, "client");

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void approveOrder_throwsOnInvalidTransition() {
        Order order = buildOrder(OrderStatus.CREATED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.approveOrder(1L))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void declineOrder_setsReasonAndStatus() {
        Order order = buildOrder(OrderStatus.AWAITING_APPROVAL);
        OrderResponse expectedResponse = new OrderResponse(
                1L, "ORD-TEST", OrderStatus.DECLINED, null,
                LocalDateTime.now(), "client", "Out of stock", List.of());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(expectedResponse);

        OrderResponse response = orderService.declineOrder(1L, "Out of stock");

        assertThat(response.getStatus()).isEqualTo(OrderStatus.DECLINED);
        assertThat(response.getDeclineReason()).isEqualTo("Out of stock");
    }

    private Order buildOrder(OrderStatus status) {
        return Order.builder()
                .id(1L)
                .orderNumber("ORD-TEST")
                .status(status)
                .client(client)
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
