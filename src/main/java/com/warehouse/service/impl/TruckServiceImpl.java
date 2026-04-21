package com.warehouse.service.impl;

import com.warehouse.dto.request.TruckRequest;
import com.warehouse.dto.response.PageResponse;
import com.warehouse.dto.response.TruckResponse;
import com.warehouse.entity.Truck;
import com.warehouse.exception.BusinessException;
import com.warehouse.exception.ResourceNotFoundException;
import com.warehouse.mapper.TruckMapper;
import com.warehouse.repository.DeliveryRepository;
import com.warehouse.repository.TruckRepository;
import com.warehouse.service.TruckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Log4j2
public class TruckServiceImpl implements TruckService {

    private final TruckRepository truckRepository;
    private final DeliveryRepository deliveryRepository;
    private final TruckMapper truckMapper;

    @Override
    public PageResponse<TruckResponse> getAllTrucks(Pageable pageable) {
        return PageResponse.from(truckRepository.findAll(pageable).map(truckMapper::toResponse));
    }

    @Override
    public TruckResponse getTruckById(Long id) {
        return truckMapper.toResponse(findById(id));
    }

    @Override
    @Transactional
    public TruckResponse createTruck(TruckRequest request) {
        if (truckRepository.existsByChassisNumber(request.getChassisNumber()))
            throw new BusinessException("Chassis number already exists: " + request.getChassisNumber());
        if (truckRepository.existsByLicensePlate(request.getLicensePlate()))
            throw new BusinessException("License plate already exists: " + request.getLicensePlate());
        Truck saved = truckRepository.save(truckMapper.toEntity(request));
        log.info("Truck created: {}", saved.getLicensePlate());
        return truckMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TruckResponse updateTruck(Long id, TruckRequest request) {
        Truck truck = findById(id);
        if (truckRepository.existsByChassisNumberAndIdNot(request.getChassisNumber(), id))
            throw new BusinessException("Chassis number already exists: " + request.getChassisNumber());
        if (truckRepository.existsByLicensePlateAndIdNot(request.getLicensePlate(), id))
            throw new BusinessException("License plate already exists: " + request.getLicensePlate());
        truckMapper.update(request, truck);
        log.info("Truck updated: {}", truck.getLicensePlate());
        return truckMapper.toResponse(truckRepository.save(truck));
    }

    @Override
    @Transactional
    public void deleteTruck(Long id) {
        Truck truck = findById(id);
        if (deliveryRepository.existsByTrucksId(id))
            throw new BusinessException("Cannot delete truck '" + truck.getLicensePlate() + "' because it is assigned to existing deliveries.");
        truckRepository.delete(truck);
        log.info("Truck deleted: {}", truck.getLicensePlate());
    }

    @Override
    public Truck findById(Long id) {
        return truckRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Truck", id));
    }
}
