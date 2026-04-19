package com.warehouse.mapper;

import com.warehouse.dto.response.OrderResponse;
import com.warehouse.dto.response.OrderSummaryResponse;
import com.warehouse.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {

    @Mapping(source = "client.username", target = "clientUsername")
    OrderResponse toResponse(Order order);

    @Mapping(source = "client.username", target = "clientUsername")
    OrderSummaryResponse toSummary(Order order);
}
