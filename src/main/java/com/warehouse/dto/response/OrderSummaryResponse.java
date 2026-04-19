package com.warehouse.dto.response;

import com.warehouse.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResponse {

    private Long id;
    private String orderNumber;
    private OrderStatus status;
    private LocalDateTime submittedDate;
    private LocalDateTime createdAt;
    private String clientUsername;
}
