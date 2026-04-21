package com.warehouse.service;

import com.warehouse.dto.request.ScheduleDeliveryRequest;
import com.warehouse.dto.response.AvailableDaysResponse;
import com.warehouse.dto.response.DeliveryResponse;

public interface DeliveryService {

    DeliveryResponse scheduleDelivery(Long orderId, ScheduleDeliveryRequest request);

    AvailableDaysResponse getAvailableDays(Long orderId, Integer days);
}
