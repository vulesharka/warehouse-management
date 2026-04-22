package com.warehouse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehouse.dto.request.TruckRequest;
import com.warehouse.dto.response.PageResponse;
import com.warehouse.dto.response.TruckResponse;
import com.warehouse.exception.BusinessException;
import com.warehouse.exception.GlobalExceptionHandler;
import com.warehouse.exception.ResourceNotFoundException;
import com.warehouse.service.TruckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
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
class TruckControllerTest {

    @Mock TruckService truckService;
    @InjectMocks TruckController truckController;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    private TruckResponse truckResponse;
    private TruckRequest truckRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(truckController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        truckResponse = new TruckResponse(1L, "CH-001", "AB-1234", new BigDecimal("20.0"));
        truckRequest = new TruckRequest("CH-001", "AB-1234", new BigDecimal("20.0"));
    }

    @Test
    void getAllTrucks_returns200WithPage() throws Exception {
        PageResponse<TruckResponse> page = PageResponse.from(new PageImpl<>(List.of(truckResponse)));
        when(truckService.getAllTrucks(any())).thenReturn(page);

        mockMvc.perform(get("/api/manager/trucks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].licensePlate").value("AB-1234"))
                .andExpect(jsonPath("$.content[0].chassisNumber").value("CH-001"));
    }

    @Test
    void getTruck_returns200WhenFound() throws Exception {
        when(truckService.getTruckById(1L)).thenReturn(truckResponse);

        mockMvc.perform(get("/api/manager/trucks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.licensePlate").value("AB-1234"));
    }

    @Test
    void getTruck_returns404WhenNotFound() throws Exception {
        when(truckService.getTruckById(99L))
                .thenThrow(new ResourceNotFoundException("Truck", 99L));

        mockMvc.perform(get("/api/manager/trucks/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTruck_returns201WithCreatedTruck() throws Exception {
        when(truckService.createTruck(any())).thenReturn(truckResponse);

        mockMvc.perform(post("/api/manager/trucks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(truckRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chassisNumber").value("CH-001"));
    }

    @Test
    void createTruck_returns400WhenBodyInvalid() throws Exception {
        mockMvc.perform(post("/api/manager/trucks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTruck_returns400WhenDuplicateChassis() throws Exception {
        when(truckService.createTruck(any()))
                .thenThrow(new BusinessException("Chassis number already exists: CH-001"));

        mockMvc.perform(post("/api/manager/trucks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(truckRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTruck_returns200WithUpdatedTruck() throws Exception {
        when(truckService.updateTruck(eq(1L), any())).thenReturn(truckResponse);

        mockMvc.perform(put("/api/manager/trucks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(truckRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateTruck_returns404WhenNotFound() throws Exception {
        when(truckService.updateTruck(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Truck", 99L));

        mockMvc.perform(put("/api/manager/trucks/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(truckRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTruck_returns204() throws Exception {
        doNothing().when(truckService).deleteTruck(1L);

        mockMvc.perform(delete("/api/manager/trucks/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTruck_returns404WhenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Truck", 99L))
                .when(truckService).deleteTruck(99L);

        mockMvc.perform(delete("/api/manager/trucks/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTruck_returns400WhenAssignedToDelivery() throws Exception {
        doThrow(new BusinessException("Cannot delete truck 'AB-1234' because it is assigned to existing deliveries."))
                .when(truckService).deleteTruck(1L);

        mockMvc.perform(delete("/api/manager/trucks/1"))
                .andExpect(status().isBadRequest());
    }
}
