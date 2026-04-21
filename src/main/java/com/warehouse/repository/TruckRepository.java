package com.warehouse.repository;

import com.warehouse.entity.Truck;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TruckRepository extends JpaRepository<Truck, Long> {

    boolean existsByChassisNumber(String chassisNumber);

    boolean existsByLicensePlate(String licensePlate);

    boolean existsByChassisNumberAndIdNot(String chassisNumber, Long id);

    boolean existsByLicensePlateAndIdNot(String licensePlate, Long id);
}
