package com.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TruckRequest {

    @NotBlank
    private String chassisNumber;

    @NotBlank
    private String licensePlate;

    @NotNull
    @Positive
    private BigDecimal containerVolume;
}
