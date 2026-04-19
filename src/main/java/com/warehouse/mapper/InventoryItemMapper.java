package com.warehouse.mapper;

import com.warehouse.dto.response.InventoryItemResponse;
import com.warehouse.entity.InventoryItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InventoryItemMapper {
    InventoryItemResponse toResponse(InventoryItem item);
}
