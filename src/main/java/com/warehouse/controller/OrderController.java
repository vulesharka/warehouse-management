package com.warehouse.controller;

import com.warehouse.dto.request.OrderRequest;
import com.warehouse.dto.response.OrderResponse;
import com.warehouse.dto.response.OrderSummaryResponse;
import com.warehouse.dto.response.PageResponse;
import com.warehouse.enums.OrderStatus;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
@Tag(name = "Orders - Client")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request,
                                                     @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(request, user.getUsername()));
    }

    @GetMapping
    @Operation(summary = "List my orders, optionally filtered by status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<PageResponse<OrderSummaryResponse>> getMyOrders(
            @RequestParam(required = false) OrderStatus status,
            @AuthenticationPrincipal UserDetails user,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(orderService.getClientOrders(user.getUsername(), status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full detail" +
            " of one of my orders")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Order not found or does not belong to current user")
    })
    public ResponseEntity<OrderResponse> getMyOrder(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(orderService.getClientOrderDetail(id, user.getUsername()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update order items (only when CREATED or DECLINED)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request body or invalid status for update"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Order not found or does not belong to current user")
    })
    public ResponseEntity<OrderResponse> updateOrder(@PathVariable Long id,
                                                     @Valid @RequestBody OrderRequest request,
                                                     @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(orderService.updateOrder(id, request, user.getUsername()));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit the order for approval")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order submitted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status transition"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Order not found or does not belong to current user")
    })
    public ResponseEntity<OrderResponse> submitOrder(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(orderService.submitOrder(id, user.getUsername()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel the order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order canceled successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status transition"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Order not found or does not belong to current user")
    })
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(orderService.cancelOrder(id, user.getUsername()));
    }
}
