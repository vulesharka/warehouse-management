package com.warehouse.service.impl;

import com.warehouse.dto.request.InventoryItemRequest;
import com.warehouse.dto.response.InventoryItemResponse;
import com.warehouse.entity.InventoryItem;
import com.warehouse.exception.ResourceNotFoundException;
import com.warehouse.mapper.InventoryItemMapper;
import com.warehouse.repository.InventoryItemRepository;
import com.warehouse.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class InventoryServiceImpl implements InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryItemMapper inventoryItemMapper;

    @Override
    public List<InventoryItemResponse> getAllItems() {
        return inventoryItemRepository.findAll().stream().map(inventoryItemMapper::toResponse).toList();
    }

    @Override
    public InventoryItemResponse getItemById(Long id) {
        return inventoryItemMapper.toResponse(findById(id));
    }

    @Override
    @Transactional
    public InventoryItemResponse createItem(InventoryItemRequest request) {
        InventoryItem item = InventoryItem.builder()
                .name(request.getName())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .packageVolume(request.getPackageVolume())
                .build();
        InventoryItem saved = inventoryItemRepository.save(item);
        log.info("Created inventory item: {}", saved.getName());
        return inventoryItemMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public InventoryItemResponse updateItem(Long id, InventoryItemRequest request) {
        InventoryItem item = findById(id);
        item.setName(request.getName());
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(request.getUnitPrice());
        item.setPackageVolume(request.getPackageVolume());
        log.info("Updated inventory item: {}", item.getName());
        return inventoryItemMapper.toResponse(inventoryItemRepository.save(item));
    }

    @Override
    @Transactional
    public void deleteItem(Long id) {
        InventoryItem item = findById(id);
        inventoryItemRepository.delete(item);
        log.info("Deleted inventory item: {}", item.getName());
    }

    @Override
    public InventoryItem findById(Long id) {
        return inventoryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", id));
    }
}
