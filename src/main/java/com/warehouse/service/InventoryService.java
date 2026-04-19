package com.warehouse.service;

import com.warehouse.dto.request.InventoryItemRequest;
import com.warehouse.dto.response.InventoryItemResponse;
import com.warehouse.entity.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryService {
    Page<InventoryItemResponse> getAllItems(Pageable pageable);
    InventoryItemResponse getItemById(Long id);
    InventoryItemResponse createItem(InventoryItemRequest request);
    InventoryItemResponse updateItem(Long id, InventoryItemRequest request);
    void deleteItem(Long id);
    InventoryItem findById(Long id);
}
