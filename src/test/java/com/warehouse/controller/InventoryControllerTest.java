package com.warehouse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehouse.dto.request.InventoryItemRequest;
import com.warehouse.dto.response.InventoryItemResponse;
import com.warehouse.exception.GlobalExceptionHandler;
import com.warehouse.exception.ResourceNotFoundException;
import com.warehouse.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock InventoryService inventoryService;
    @InjectMocks InventoryController inventoryController;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    private InventoryItemResponse itemResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(inventoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        itemResponse = new InventoryItemResponse(1L, "Box A", 100,
                new BigDecimal("9.99"), new BigDecimal("0.5"));
    }

    @Test
    void getAllItems_returns200WithList() throws Exception {
        when(inventoryService.getAllItems(any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(itemResponse)));

        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Box A"))
                .andExpect(jsonPath("$.content[0].quantity").value(100));
    }

    @Test
    void getItem_returns200WhenFound() throws Exception {
        when(inventoryService.getItemById(1L)).thenReturn(itemResponse);

        mockMvc.perform(get("/api/inventory/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Box A"));
    }

    @Test
    void getItem_returns404WhenNotFound() throws Exception {
        when(inventoryService.getItemById(99L))
                .thenThrow(new ResourceNotFoundException("InventoryItem", 99L));

        mockMvc.perform(get("/api/inventory/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createItem_returns201WithCreatedItem() throws Exception {
        InventoryItemRequest request = new InventoryItemRequest("Box A", 100,
                new BigDecimal("9.99"), new BigDecimal("0.5"));
        when(inventoryService.createItem(any())).thenReturn(itemResponse);

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Box A"));
    }

    @Test
    void createItem_returns400WhenBodyInvalid() throws Exception {
        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateItem_returns200WithUpdatedItem() throws Exception {
        InventoryItemRequest request = new InventoryItemRequest("Box B", 50,
                new BigDecimal("5.00"), new BigDecimal("1.0"));
        when(inventoryService.updateItem(eq(1L), any())).thenReturn(itemResponse);

        mockMvc.perform(put("/api/inventory/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateItem_returns404WhenNotFound() throws Exception {
        InventoryItemRequest request = new InventoryItemRequest("Box B", 50,
                new BigDecimal("5.00"), new BigDecimal("1.0"));
        when(inventoryService.updateItem(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("InventoryItem", 99L));

        mockMvc.perform(put("/api/inventory/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteItem_returns204() throws Exception {
        doNothing().when(inventoryService).deleteItem(1L);

        mockMvc.perform(delete("/api/inventory/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteItem_returns404WhenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("InventoryItem", 99L))
                .when(inventoryService).deleteItem(99L);

        mockMvc.perform(delete("/api/inventory/99"))
                .andExpect(status().isNotFound());
    }
}
