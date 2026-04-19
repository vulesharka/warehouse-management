package com.warehouse.service;

import com.warehouse.dto.request.OrderRequest;
import com.warehouse.dto.response.OrderResponse;
import com.warehouse.dto.response.OrderSummaryResponse;
import com.warehouse.enums.OrderStatus;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request, String username);
    OrderResponse updateOrder(Long orderId, OrderRequest request, String username);
    OrderResponse submitOrder(Long orderId, String username);
    OrderResponse cancelOrder(Long orderId, String username);
    List<OrderSummaryResponse> getClientOrders(String username, OrderStatus status);
    OrderResponse getClientOrderDetail(Long orderId, String username);
    List<OrderSummaryResponse> getAllOrders(OrderStatus status);
    OrderResponse getOrderDetail(Long orderId);
    OrderResponse approveOrder(Long orderId);
    OrderResponse declineOrder(Long orderId, String reason);
}
