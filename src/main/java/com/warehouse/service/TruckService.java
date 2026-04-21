package com.warehouse.service;

import com.warehouse.dto.request.TruckRequest;
import com.warehouse.dto.response.PageResponse;
import com.warehouse.dto.response.TruckResponse;
import com.warehouse.entity.Truck;
import org.springframework.data.domain.Pageable;

public interface TruckService {

    PageResponse<TruckResponse> getAllTrucks(Pageable pageable);

    TruckResponse getTruckById(Long id);

    TruckResponse createTruck(TruckRequest request);

    TruckResponse updateTruck(Long id, TruckRequest request);

    void deleteTruck(Long id);

    Truck findById(Long id);
}
