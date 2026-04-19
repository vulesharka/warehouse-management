package com.warehouse.dto.response;

import com.warehouse.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private OrderStatus status;
    private LocalDateTime submittedDate;
    private LocalDateTime createdAt;
    private String clientUsername;
    private String declineReason;
    private List<OrderItemResponse> items;
}
