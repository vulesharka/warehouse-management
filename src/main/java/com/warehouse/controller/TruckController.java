package com.warehouse.controller;

import com.warehouse.dto.request.TruckRequest;
import com.warehouse.dto.response.PageResponse;
import com.warehouse.dto.response.TruckResponse;
import com.warehouse.service.TruckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/trucks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('WAREHOUSE_MANAGER')")
@Tag(name = "Trucks - Manager")
public class TruckController {

    private final TruckService truckService;

    @GetMapping
    @Operation(summary = "List all trucks")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Trucks retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<PageResponse<TruckResponse>> getAllTrucks(
            @ParameterObject @PageableDefault(size = 20, sort = "licensePlate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(truckService.getAllTrucks(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a truck by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Truck retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Truck not found")
    })
    public ResponseEntity<TruckResponse> getTruck(@PathVariable Long id) {
        return ResponseEntity.ok(truckService.getTruckById(id));
    }

    @PostMapping
    @Operation(summary = "Add a new truck")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Truck created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request body or duplicate chassis/plate"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<TruckResponse> createTruck(@Valid @RequestBody TruckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(truckService.createTruck(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a truck")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Truck updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request body or duplicate chassis/plate"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Truck not found")
    })
    public ResponseEntity<TruckResponse> updateTruck(@PathVariable Long id,
                                                     @Valid @RequestBody TruckRequest request) {
        return ResponseEntity.ok(truckService.updateTruck(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a truck")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Truck deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Truck not found")
    })
    public ResponseEntity<Void> deleteTruck(@PathVariable Long id) {
        truckService.deleteTruck(id);
        return ResponseEntity.noContent().build();
    }
}
