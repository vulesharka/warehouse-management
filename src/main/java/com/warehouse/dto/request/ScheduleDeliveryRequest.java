package com.warehouse.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDeliveryRequest {

    @NotNull
    private LocalDate deliveryDate;

    @NotEmpty
    private List<Long> truckIds;
}
