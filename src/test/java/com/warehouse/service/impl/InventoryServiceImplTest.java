package com.warehouse.service.impl;

import com.warehouse.dto.request.InventoryItemRequest;
import com.warehouse.dto.response.InventoryItemResponse;
import com.warehouse.entity.InventoryItem;
import com.warehouse.exception.ResourceNotFoundException;
import com.warehouse.mapper.InventoryItemMapper;
import com.warehouse.repository.InventoryItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock InventoryItemRepository inventoryItemRepository;
    @Mock InventoryItemMapper inventoryItemMapper;

    @InjectMocks InventoryServiceImpl inventoryService;

    @Test
    void createItem_savesAndReturnsItem() {
        InventoryItemRequest request = new InventoryItemRequest(
                "Box A", 100, new BigDecimal("9.99"), new BigDecimal("0.5"));
        InventoryItem saved = InventoryItem.builder()
                .id(1L).name("Box A").quantity(100)
                .unitPrice(new BigDecimal("9.99")).packageVolume(new BigDecimal("0.5"))
                .build();
        InventoryItemResponse response = new InventoryItemResponse(
                1L, "Box A", 100, new BigDecimal("9.99"), new BigDecimal("0.5"));

        when(inventoryItemRepository.save(any())).thenReturn(saved);
        when(inventoryItemMapper.toResponse(saved)).thenReturn(response);

        InventoryItemResponse result = inventoryService.createItem(request);

        assertThat(result.getName()).isEqualTo("Box A");
        assertThat(result.getQuantity()).isEqualTo(100);
    }

    @Test
    void getItemById_throwsWhenNotFound() {
        when(inventoryItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getItemById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deleteItem_deletesSuccessfully() {
        InventoryItem item = InventoryItem.builder().id(1L).name("Box A").build();
        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(item));

        inventoryService.deleteItem(1L);

        verify(inventoryItemRepository).delete(item);
    }

    @Test
    void updateItem_updatesFields() {
        InventoryItem existing = InventoryItem.builder()
                .id(1L).name("Old").quantity(10)
                .unitPrice(BigDecimal.ONE).packageVolume(BigDecimal.ONE)
                .build();
        InventoryItemRequest request = new InventoryItemRequest(
                "New", 20, new BigDecimal("5.00"), new BigDecimal("1.0"));
        InventoryItemResponse response = new InventoryItemResponse(
                1L, "New", 20, new BigDecimal("5.00"), new BigDecimal("1.0"));

        when(inventoryItemRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryItemMapper.toResponse(any())).thenReturn(response);

        InventoryItemResponse result = inventoryService.updateItem(1L, request);

        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getQuantity()).isEqualTo(20);
    }
}
