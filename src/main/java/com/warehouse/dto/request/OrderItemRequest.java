package com.warehouse.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {

    @NotNull
    private Long inventoryItemId;

    @NotNull
    @Min(1)
    private Integer requestedQuantity;

    @NotNull
    private LocalDate deadlineDate;
}
