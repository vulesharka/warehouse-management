package com.warehouse.service.impl;

import com.warehouse.dto.request.TruckRequest;
import com.warehouse.dto.response.TruckResponse;
import com.warehouse.entity.Truck;
import com.warehouse.exception.BusinessException;
import com.warehouse.exception.ResourceNotFoundException;
import com.warehouse.mapper.TruckMapper;
import com.warehouse.repository.DeliveryRepository;
import com.warehouse.repository.TruckRepository;
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
class TruckServiceImplTest {

    @Mock TruckRepository truckRepository;
    @Mock DeliveryRepository deliveryRepository;
    @Mock TruckMapper truckMapper;

    @InjectMocks TruckServiceImpl truckService;

    private static final TruckRequest REQUEST =
            new TruckRequest("CH-001", "AB-1234", new BigDecimal("20.0"));

    private Truck truck(Long id) {
        return Truck.builder()
                .id(id).chassisNumber("CH-001")
                .licensePlate("AB-1234").containerVolume(new BigDecimal("20.0"))
                .build();
    }

    private TruckResponse response(Long id) {
        return new TruckResponse(id, "CH-001", "AB-1234", new BigDecimal("20.0"));
    }

    @Test
    void createTruck_savesAndReturnsResponse() {
        Truck saved = truck(1L);
        when(truckRepository.existsByChassisNumber("CH-001")).thenReturn(false);
        when(truckRepository.existsByLicensePlate("AB-1234")).thenReturn(false);
        when(truckMapper.toEntity(REQUEST)).thenReturn(saved);
        when(truckRepository.save(saved)).thenReturn(saved);
        when(truckMapper.toResponse(saved)).thenReturn(response(1L));

        TruckResponse result = truckService.createTruck(REQUEST);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getLicensePlate()).isEqualTo("AB-1234");
    }

    @Test
    void createTruck_throwsWhenDuplicateChassis() {
        when(truckRepository.existsByChassisNumber("CH-001")).thenReturn(true);

        assertThatThrownBy(() -> truckService.createTruck(REQUEST))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CH-001");
        verify(truckRepository, never()).save(any());
    }

    @Test
    void createTruck_throwsWhenDuplicateLicensePlate() {
        when(truckRepository.existsByChassisNumber("CH-001")).thenReturn(false);
        when(truckRepository.existsByLicensePlate("AB-1234")).thenReturn(true);

        assertThatThrownBy(() -> truckService.createTruck(REQUEST))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AB-1234");
        verify(truckRepository, never()).save(any());
    }

    @Test
    void getTruckById_throwsWhenNotFound() {
        when(truckRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> truckService.getTruckById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateTruck_updatesAndReturnsResponse() {
        Truck existing = truck(1L);
        when(truckRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(truckRepository.existsByChassisNumberAndIdNot("CH-001", 1L)).thenReturn(false);
        when(truckRepository.existsByLicensePlateAndIdNot("AB-1234", 1L)).thenReturn(false);
        when(truckRepository.save(existing)).thenReturn(existing);
        when(truckMapper.toResponse(existing)).thenReturn(response(1L));

        TruckResponse result = truckService.updateTruck(1L, REQUEST);

        assertThat(result.getChassisNumber()).isEqualTo("CH-001");
        verify(truckMapper).update(REQUEST, existing);
    }

    @Test
    void deleteTruck_deletesSuccessfully() {
        Truck truck = truck(1L);
        when(truckRepository.findById(1L)).thenReturn(Optional.of(truck));
        when(deliveryRepository.existsByTrucksId(1L)).thenReturn(false);

        truckService.deleteTruck(1L);

        verify(truckRepository).delete(truck);
    }

    @Test
    void deleteTruck_throwsWhenAssignedToDelivery() {
        Truck truck = truck(1L);
        when(truckRepository.findById(1L)).thenReturn(Optional.of(truck));
        when(deliveryRepository.existsByTrucksId(1L)).thenReturn(true);

        assertThatThrownBy(() -> truckService.deleteTruck(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AB-1234");
        verify(truckRepository, never()).delete(any());
    }
}
