package com.warehouse.repository;

import com.warehouse.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    boolean existsByOrderId(Long orderId);

    boolean existsByTrucksId(Long truckId);

    @Query("SELECT t.id FROM Delivery d JOIN d.trucks t WHERE d.deliveryDate = :date")
    List<Long> findTruckIdsByDeliveryDate(@Param("date") LocalDate date);

    @Query("SELECT d FROM Delivery d JOIN FETCH d.order o WHERE d.deliveryDate <= :today AND o.status = 'UNDER_DELIVERY'")
    List<Delivery> findDueDeliveries(@Param("today") LocalDate today);
}
