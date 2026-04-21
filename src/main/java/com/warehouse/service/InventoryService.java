package com.warehouse.service;

import com.warehouse.dto.request.InventoryItemRequest;
import com.warehouse.dto.response.InventoryItemResponse;
import com.warehouse.entity.InventoryItem;

import java.util.List;

public interface InventoryService {
    List<InventoryItemResponse> getAllItems();
    InventoryItemResponse getItemById(Long id);
    InventoryItemResponse createItem(InventoryItemRequest request);
    InventoryItemResponse updateItem(Long id, InventoryItemRequest request);
    void deleteItem(Long id);
    InventoryItem findById(Long id);
}
