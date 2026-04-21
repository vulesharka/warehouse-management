package com.warehouse.service.impl;

import com.warehouse.dto.request.ScheduleDeliveryRequest;
import com.warehouse.dto.response.AvailableDaysResponse;
import com.warehouse.dto.response.DeliveryResponse;
import com.warehouse.entity.Delivery;
import com.warehouse.entity.InventoryItem;
import com.warehouse.entity.Order;
import com.warehouse.entity.OrderItem;
import com.warehouse.entity.Truck;
import com.warehouse.enums.OrderStatus;
import com.warehouse.exception.BusinessException;
import com.warehouse.exception.InvalidStatusTransitionException;
import com.warehouse.exception.ResourceNotFoundException;
import com.warehouse.mapper.DeliveryMapper;
import com.warehouse.repository.DeliveryRepository;
import com.warehouse.repository.InventoryItemRepository;
import com.warehouse.repository.OrderRepository;
import com.warehouse.repository.TruckRepository;
import com.warehouse.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Log4j2
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final TruckRepository truckRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final DeliveryMapper deliveryMapper;

    @Value("${app.delivery.max-period-days:30}")
    private int maxPeriodDays;

    @Override
    @Transactional
    public DeliveryResponse scheduleDelivery(Long orderId, ScheduleDeliveryRequest request) {
        Order order = findOrderById(orderId);

        if (order.getStatus() != OrderStatus.APPROVED)
            throw new InvalidStatusTransitionException(order.getStatus(), OrderStatus.UNDER_DELIVERY);

        if (deliveryRepository.existsByOrderId(orderId))
            throw new BusinessException("Order already has a scheduled delivery");

        validateDeliveryDate(request.getDeliveryDate());

        List<Truck> trucks = resolveTrucks(request.getTruckIds());
        validateTrucksAvailable(trucks, request.getDeliveryDate());

        BigDecimal orderVolume = calculateOrderVolume(order);
        BigDecimal truckCapacity = combinedVolume(trucks);
        if (truckCapacity.compareTo(orderVolume) < 0)
            throw new BusinessException(
                    "Insufficient truck capacity: needed " + orderVolume + ", available " + truckCapacity);

        deductInventory(order);

        order.setStatus(OrderStatus.UNDER_DELIVERY);
        orderRepository.save(order);

        Delivery delivery = Delivery.builder()
                .order(order)
                .deliveryDate(request.getDeliveryDate())
                .trucks(Set.copyOf(trucks))
                .build();
        Delivery saved = deliveryRepository.save(delivery);

        log.info("Delivery scheduled for order {} on {}", order.getOrderNumber(), request.getDeliveryDate());
        return deliveryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AvailableDaysResponse getAvailableDays(Long orderId, Integer days) {
        Order order = findOrderById(orderId);
        if (order.getStatus() != OrderStatus.APPROVED)
            throw new BusinessException("Available days can only be queried for APPROVED orders");

        int lookAheadDays = (days != null && days > 0) ? Math.min(days, maxPeriodDays) : maxPeriodDays;
        BigDecimal orderVolume = calculateOrderVolume(order);
        List<Truck> allTrucks = truckRepository.findAll();

        List<LocalDate> availableDays = new ArrayList<>();
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(lookAheadDays);

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (isWeekend(date)) continue;

            List<Long> bookedTruckIds = deliveryRepository.findTruckIdsByDeliveryDate(date);
            BigDecimal availableVolume = allTrucks.stream()
                    .filter(t -> !bookedTruckIds.contains(t.getId()))
                    .map(Truck::getContainerVolume)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (availableVolume.compareTo(orderVolume) >= 0)
                availableDays.add(date);
        }

        return new AvailableDaysResponse(availableDays);
    }

    // ── Helpers

    private void validateDeliveryDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (!date.isAfter(today))
            throw new BusinessException("Delivery date must be in the future");
        if (isWeekend(date))
            throw new BusinessException("Delivery date cannot be on a weekend");
        if (date.isAfter(today.plusDays(maxPeriodDays)))
            throw new BusinessException("Delivery date must be within " + maxPeriodDays + " days from today");
    }

    private List<Truck> resolveTrucks(List<Long> truckIds) {
        List<Truck> trucks = truckRepository.findAllById(truckIds);
        if (trucks.size() != truckIds.size()) {
            List<Long> found = trucks.stream().map(Truck::getId).toList();
            List<Long> missing = truckIds.stream().filter(id -> !found.contains(id)).toList();
            throw new BusinessException("Trucks not found: " + missing);
        }
        return trucks;
    }

    private void validateTrucksAvailable(List<Truck> trucks, LocalDate date) {
        List<Long> bookedIds = deliveryRepository.findTruckIdsByDeliveryDate(date);
        List<Long> conflicting = trucks.stream()
                .map(Truck::getId)
                .filter(bookedIds::contains)
                .toList();
        if (!conflicting.isEmpty())
            throw new BusinessException("Trucks already booked on " + date + ": " + conflicting);
    }

    private BigDecimal calculateOrderVolume(Order order) {
        return order.getItems().stream()
                .map(item -> item.getInventoryItem().getPackageVolume()
                        .multiply(BigDecimal.valueOf(item.getRequestedQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal combinedVolume(List<Truck> trucks) {
        return trucks.stream()
                .map(Truck::getContainerVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void deductInventory(Order order) {
        for (OrderItem item : order.getItems()) {
            InventoryItem inv = item.getInventoryItem();
            int newQty = inv.getQuantity() - item.getRequestedQuantity();
            if (newQty < 0)
                throw new BusinessException("Insufficient stock for item: " + inv.getName());
            inv.setQuantity(newQty);
            inventoryItemRepository.save(inv);
        }
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }
}
