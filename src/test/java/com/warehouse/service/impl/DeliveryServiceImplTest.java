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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    @Mock DeliveryRepository deliveryRepository;
    @Mock OrderRepository orderRepository;
    @Mock TruckRepository truckRepository;
    @Mock InventoryItemRepository inventoryItemRepository;
    @Mock DeliveryMapper deliveryMapper;

    @InjectMocks DeliveryServiceImpl deliveryService;

    private static final LocalDate FUTURE_WEEKDAY = nextWeekday();

    private static LocalDate nextWeekday() {
        LocalDate d = LocalDate.now().plusDays(1);
        while (d.getDayOfWeek().getValue() >= 6) d = d.plusDays(1);
        return d;
    }

    private Order approvedOrder() {
        InventoryItem item = InventoryItem.builder()
                .id(1L).name("Box").quantity(10)
                .packageVolume(new BigDecimal("2.0"))
                .build();
        OrderItem orderItem = OrderItem.builder()
                .id(1L).inventoryItem(item).requestedQuantity(2)
                .deadlineDate(FUTURE_WEEKDAY)
                .build();
        Order order = Order.builder()
                .id(1L).orderNumber("ORD-001").status(OrderStatus.APPROVED)
                .items(List.of(orderItem))
                .build();
        orderItem.setOrder(order);
        return order;
    }

    private Truck truck(Long id, BigDecimal volume) {
        return Truck.builder().id(id).chassisNumber("CH-00" + id)
                .licensePlate("PL-00" + id).containerVolume(volume).build();
    }

    @Test
    void scheduleDelivery_success() {
        ReflectionTestUtils.setField(deliveryService, "maxPeriodDays", 30);
        Order order = approvedOrder();
        Truck truck = truck(1L, new BigDecimal("10.0"));
        ScheduleDeliveryRequest request = new ScheduleDeliveryRequest(FUTURE_WEEKDAY, List.of(1L));
        DeliveryResponse expected = new DeliveryResponse(1L, 1L, "ORD-001", FUTURE_WEEKDAY, List.of());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(deliveryRepository.existsByOrderId(1L)).thenReturn(false);
        when(truckRepository.findAllById(List.of(1L))).thenReturn(List.of(truck));
        when(deliveryRepository.findTruckIdsByDeliveryDate(FUTURE_WEEKDAY)).thenReturn(List.of());
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any())).thenReturn(Delivery.builder().id(1L).build());
        when(deliveryMapper.toResponse(any())).thenReturn(expected);

        DeliveryResponse result = deliveryService.scheduleDelivery(1L, request);

        assertThat(result.getOrderNumber()).isEqualTo("ORD-001");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.UNDER_DELIVERY);
    }

    @Test
    void scheduleDelivery_throwsWhenOrderNotApproved() {
        ReflectionTestUtils.setField(deliveryService, "maxPeriodDays", 30);
        Order order = Order.builder().id(1L).orderNumber("ORD-001")
                .status(OrderStatus.AWAITING_APPROVAL).items(List.of()).build();
        ScheduleDeliveryRequest request = new ScheduleDeliveryRequest(FUTURE_WEEKDAY, List.of(1L));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> deliveryService.scheduleDelivery(1L, request))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void scheduleDelivery_throwsWhenOrderNotFound() {
        ReflectionTestUtils.setField(deliveryService, "maxPeriodDays", 30);
        ScheduleDeliveryRequest request = new ScheduleDeliveryRequest(FUTURE_WEEKDAY, List.of(1L));
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.scheduleDelivery(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void scheduleDelivery_throwsWhenTruckAlreadyBooked() {
        ReflectionTestUtils.setField(deliveryService, "maxPeriodDays", 30);
        Order order = approvedOrder();
        Truck truck = truck(1L, new BigDecimal("10.0"));
        ScheduleDeliveryRequest request = new ScheduleDeliveryRequest(FUTURE_WEEKDAY, List.of(1L));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(deliveryRepository.existsByOrderId(1L)).thenReturn(false);
        when(truckRepository.findAllById(List.of(1L))).thenReturn(List.of(truck));
        when(deliveryRepository.findTruckIdsByDeliveryDate(FUTURE_WEEKDAY)).thenReturn(List.of(1L));

        assertThatThrownBy(() -> deliveryService.scheduleDelivery(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already booked");
    }

    @Test
    void scheduleDelivery_throwsWhenInsufficientTruckCapacity() {
        ReflectionTestUtils.setField(deliveryService, "maxPeriodDays", 30);
        Order order = approvedOrder();
        Truck truck = truck(1L, new BigDecimal("1.0")); // order needs 4.0 (2 items * 2.0)
        ScheduleDeliveryRequest request = new ScheduleDeliveryRequest(FUTURE_WEEKDAY, List.of(1L));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(deliveryRepository.existsByOrderId(1L)).thenReturn(false);
        when(truckRepository.findAllById(List.of(1L))).thenReturn(List.of(truck));
        when(deliveryRepository.findTruckIdsByDeliveryDate(FUTURE_WEEKDAY)).thenReturn(List.of());

        assertThatThrownBy(() -> deliveryService.scheduleDelivery(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("capacity");
    }

    @Test
    void scheduleDelivery_throwsWhenInsufficientStock() {
        ReflectionTestUtils.setField(deliveryService, "maxPeriodDays", 30);
        InventoryItem item = InventoryItem.builder()
                .id(1L).name("Box").quantity(1) // only 1 in stock
                .packageVolume(new BigDecimal("2.0"))
                .build();
        OrderItem orderItem = OrderItem.builder()
                .id(1L).inventoryItem(item).requestedQuantity(5) // requesting 5
                .deadlineDate(FUTURE_WEEKDAY)
                .build();
        Order order = Order.builder()
                .id(1L).orderNumber("ORD-001").status(OrderStatus.APPROVED)
                .items(List.of(orderItem))
                .build();
        orderItem.setOrder(order);
        Truck truck = truck(1L, new BigDecimal("100.0"));
        ScheduleDeliveryRequest request = new ScheduleDeliveryRequest(FUTURE_WEEKDAY, List.of(1L));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(deliveryRepository.existsByOrderId(1L)).thenReturn(false);
        when(truckRepository.findAllById(List.of(1L))).thenReturn(List.of(truck));
        when(deliveryRepository.findTruckIdsByDeliveryDate(FUTURE_WEEKDAY)).thenReturn(List.of());

        assertThatThrownBy(() -> deliveryService.scheduleDelivery(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void getAvailableDays_returnsWeekdaysWithEnoughCapacity() {
        ReflectionTestUtils.setField(deliveryService, "maxPeriodDays", 7);
        Order order = approvedOrder(); // volume needed: 4.0
        Truck truck = truck(1L, new BigDecimal("10.0"));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(truckRepository.findAll()).thenReturn(List.of(truck));
        when(deliveryRepository.findTruckIdsByDeliveryDate(any())).thenReturn(List.of());

        AvailableDaysResponse result = deliveryService.getAvailableDays(1L, null);

        assertThat(result.getAvailableDays()).isNotEmpty();
        result.getAvailableDays().forEach(d -> {
            assertThat(d.getDayOfWeek().getValue()).isLessThan(6);
            assertThat(d).isAfter(LocalDate.now());
        });
    }

    @Test
    void getAvailableDays_throwsWhenOrderNotApproved() {
        ReflectionTestUtils.setField(deliveryService, "maxPeriodDays", 30);
        Order order = Order.builder().id(1L).orderNumber("ORD-001")
                .status(OrderStatus.AWAITING_APPROVAL).items(List.of()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> deliveryService.getAvailableDays(1L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("APPROVED");
    }
}
