package com.warehouse.service.impl;

import com.warehouse.dto.request.OrderItemRequest;
import com.warehouse.dto.request.OrderRequest;
import com.warehouse.dto.response.OrderResponse;
import com.warehouse.dto.response.OrderSummaryResponse;
import com.warehouse.entity.InventoryItem;
import com.warehouse.entity.Order;
import com.warehouse.entity.OrderItem;
import com.warehouse.entity.User;
import com.warehouse.enums.OrderStatus;
import com.warehouse.exception.BusinessException;
import com.warehouse.exception.InvalidStatusTransitionException;
import com.warehouse.exception.ResourceNotFoundException;
import com.warehouse.mapper.OrderMapper;
import com.warehouse.repository.OrderRepository;
import com.warehouse.repository.UserRepository;
import com.warehouse.service.InventoryService;
import com.warehouse.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final OrderMapper orderMapper;

    // ── CLIENT

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request, String username) {
        User client = loadUser(username);
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .status(OrderStatus.CREATED)
                .client(client)
                .build();
        applyItems(order, request.getItems());
        Order saved = orderRepository.save(order);
        log.info("Order created: {} by {}", saved.getOrderNumber(), username);
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(Long orderId, OrderRequest request, String username) {
        Order order = findOwnedOrder(orderId, username);
        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.DECLINED) {
            throw new BusinessException("Order can only be updated when in CREATED or DECLINED status");
        }
        order.getItems().clear();
        applyItems(order, request.getItems());
        log.info("Order updated: {}", order.getOrderNumber());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse submitOrder(Long orderId, String username) {
        Order order = findOwnedOrder(orderId, username);
        validateTransition(order.getStatus(), OrderStatus.AWAITING_APPROVAL);
        order.setStatus(OrderStatus.AWAITING_APPROVAL);
        order.setSubmittedDate(LocalDateTime.now());
        log.info("Order submitted: {}", order.getOrderNumber());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId, String username) {
        Order order = findOwnedOrder(orderId, username);
        validateTransition(order.getStatus(), OrderStatus.CANCELED);
        order.setStatus(OrderStatus.CANCELED);
        log.info("Order canceled: {}", order.getOrderNumber());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public Page<OrderSummaryResponse> getClientOrders(String username, OrderStatus status, Pageable pageable) {
        User client = loadUser(username);
        Page<Order> orders = status != null
                ? orderRepository.findByClientAndStatusOrderByCreatedAtDesc(client, status, pageable)
                : orderRepository.findByClientOrderByCreatedAtDesc(client, pageable);
        return orders.map(orderMapper::toSummary);
    }

    @Override
    @Transactional
    public OrderResponse getClientOrderDetail(Long orderId, String username) {
        return orderMapper.toResponse(findOwnedOrder(orderId, username));
    }

    // ── WAREHOUSE MANAGER

    @Override
    @Transactional
    public Page<OrderSummaryResponse> getAllOrders(OrderStatus status, Pageable pageable) {
        Page<Order> orders = status != null
                ? orderRepository.findByStatusSortedBySubmittedDate(status, pageable)
                : orderRepository.findAllSortedBySubmittedDate(pageable);
        return orders.map(orderMapper::toSummary);
    }

    @Override
    @Transactional
    public OrderResponse getOrderDetail(Long orderId) {
        return orderMapper.toResponse(findById(orderId));
    }

    @Override
    @Transactional
    public OrderResponse approveOrder(Long orderId) {
        Order order = findById(orderId);
        validateTransition(order.getStatus(), OrderStatus.APPROVED);
        order.setStatus(OrderStatus.APPROVED);
        log.info("Order approved: {}", order.getOrderNumber());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse declineOrder(Long orderId, String reason) {
        Order order = findById(orderId);
        validateTransition(order.getStatus(), OrderStatus.DECLINED);
        order.setStatus(OrderStatus.DECLINED);
        order.setDeclineReason(reason);
        log.info("Order declined: {}", order.getOrderNumber());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    // ── Helpers

    private void applyItems(Order order, List<OrderItemRequest> itemRequests) {
        for (OrderItemRequest req : itemRequests) {
            InventoryItem inventoryItem = inventoryService.findById(req.getInventoryItemId());
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .inventoryItem(inventoryItem)
                    .requestedQuantity(req.getRequestedQuantity())
                    .deadlineDate(req.getDeadlineDate())
                    .build();
            order.getItems().add(orderItem);
        }
    }

    private void validateTransition(OrderStatus current, OrderStatus next) {
        if (!current.canTransitionTo(next))
            throw new InvalidStatusTransitionException(current, next);
    }

    private Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    private Order findOwnedOrder(Long orderId, String username) {
        User client = loadUser(username);
        return orderRepository.findByIdAndClient(orderId, client)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    private User loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
