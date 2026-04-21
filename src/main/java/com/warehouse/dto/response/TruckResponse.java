package com.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TruckResponse {

    private Long id;
    private String chassisNumber;
    private String licensePlate;
    private BigDecimal containerVolume;
}
