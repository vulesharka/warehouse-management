package com.warehouse.mapper;

import com.warehouse.dto.response.DeliveryResponse;
import com.warehouse.entity.Delivery;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = TruckMapper.class)
public interface DeliveryMapper {

    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "order.orderNumber", target = "orderNumber")
    DeliveryResponse toResponse(Delivery delivery);
}
