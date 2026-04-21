package com.warehouse.controller;

import com.warehouse.dto.request.DeclineRequest;
import com.warehouse.dto.request.ScheduleDeliveryRequest;
import com.warehouse.dto.response.AvailableDaysResponse;
import com.warehouse.dto.response.DeliveryResponse;
import com.warehouse.dto.response.OrderResponse;
import com.warehouse.dto.response.OrderSummaryResponse;
import com.warehouse.dto.response.PageResponse;
import com.warehouse.enums.OrderStatus;
import com.warehouse.service.DeliveryService;
import com.warehouse.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('WAREHOUSE_MANAGER')")
@Tag(name = "Orders - Manager")
public class ManagerOrderController {

    private final OrderService orderService;
    private final DeliveryService deliveryService;

    @GetMapping
    @Operation(summary = "List all orders (basic info), optionally filtered by status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<PageResponse<OrderSummaryResponse>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "submittedDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(orderService.getAllOrders(status, pageable)));
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

    @PostMapping("/{id}/schedule")
    @Operation(summary = "Schedule delivery for an approved order")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Delivery scheduled successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date, trucks unavailable, or insufficient capacity"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Order or truck not found")
    })
    public ResponseEntity<DeliveryResponse> scheduleDelivery(@PathVariable Long id,
                                                             @Valid @RequestBody ScheduleDeliveryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deliveryService.scheduleDelivery(id, request));
    }

    @GetMapping("/{id}/available-days")
    @Operation(summary = "Get available delivery dates for an approved order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Available days retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Order is not in APPROVED status"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<AvailableDaysResponse> getAvailableDays(
            @PathVariable Long id,
            @RequestParam(required = false) Integer days) {
        return ResponseEntity.ok(deliveryService.getAvailableDays(id, days));
    }
}
