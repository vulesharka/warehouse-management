package com.warehouse.mapper;

import com.warehouse.dto.response.OrderItemResponse;
import com.warehouse.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    @Mapping(source = "inventoryItem.id",   target = "inventoryItemId")
    @Mapping(source = "inventoryItem.name", target = "itemName")
    OrderItemResponse toResponse(OrderItem item);
}
