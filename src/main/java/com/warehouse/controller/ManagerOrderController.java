package com.warehouse.controller;

import com.warehouse.dto.request.DeclineRequest;
import com.warehouse.dto.response.OrderResponse;
import com.warehouse.dto.response.OrderSummaryResponse;
import com.warehouse.enums.OrderStatus;
import com.warehouse.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('WAREHOUSE_MANAGER')")
@Tag(name = "Orders - Manager")
public class ManagerOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "List all orders (basic info), optionally filtered by status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<OrderSummaryResponse>> getAllOrders(
            @RequestParam(required = false) OrderStatus status) {
        return ResponseEntity.ok(orderService.getAllOrders(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed information about a specific order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> getOrderDetail(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderDetail(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve an order awaiting approval")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order approved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status transition"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> approveOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.approveOrder(id));
    }

    @PostMapping("/{id}/decline")
    @Operation(summary = "Decline an order awaiting approval, with optional reason")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order declined successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status transition"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> declineOrder(@PathVariable Long id,
                                                      @RequestBody(required = false) DeclineRequest request) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(orderService.declineOrder(id, reason));
    }
}
