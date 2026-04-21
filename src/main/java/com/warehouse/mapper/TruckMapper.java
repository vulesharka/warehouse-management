package com.warehouse.mapper;

import com.warehouse.dto.request.TruckRequest;
import com.warehouse.dto.response.TruckResponse;
import com.warehouse.entity.Truck;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TruckMapper {

    TruckResponse toResponse(Truck truck);

    @Mapping(target = "id", ignore = true)
    Truck toEntity(TruckRequest request);

    @Mapping(target = "id", ignore = true)
    void update(TruckRequest request, @MappingTarget Truck truck);
}
