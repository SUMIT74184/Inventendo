package org.example.warehousemcs.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.warehousemcs.Dto.WarehouseDTO;
import org.example.warehousemcs.Dto.WarehouseRequest;
import org.example.warehousemcs.service.WarehouseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;


    @PostMapping
    public ResponseEntity<WarehouseDTO> createWarehouse(@Valid @RequestBody WarehouseRequest request){
        return new ResponseEntity<>(warehouseService.createWarehouse(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseDTO> getWarehouseById(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") String tenantId
    ){
        return ResponseEntity.ok(warehouseService.getWarehouseById(id,tenantId));
    }


    @GetMapping("/code/{warehouseCode}")
    public ResponseEntity<WarehouseDTO> getWarehouseByCode(
            @PathVariable String warehouseCode,
            @RequestHeader("X-Tenant-Id") String tenantId
    ){
        return ResponseEntity.ok(warehouseService.getWarehouseByCode(warehouseCode,tenantId));
    }

    @GetMapping
    public ResponseEntity<List<WarehouseDTO>>getAllWarehouses(
            @RequestHeader("X-Tenant-Id") String tenantId
    ){
        return ResponseEntity.ok(warehouseService.getAllWarehouses(tenantId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<WarehouseDTO>>getActiveWarehouse(
            @RequestHeader("X-Tenant-Id") String tenantId
    ){
        return ResponseEntity.ok(warehouseService.getActiveWarehouses(tenantId));
    }


    @GetMapping("/status/{status}")
    public ResponseEntity<List<WarehouseDTO>>getWarehouseByStatus(
            @PathVariable String status,
            @RequestHeader("X-Tenant-Id") String tenantId
    ){
        return ResponseEntity.ok(warehouseService.getWarehouseByStatus(status,tenantId));
    }


    @GetMapping("/location/{location}")
    public ResponseEntity<List<WarehouseDTO>> getWarehouseByLocation(
            @PathVariable String location,
            @RequestHeader("X-Tenant-Id") String tenantId
    ){
        return ResponseEntity.ok(warehouseService.getWarehousesByLocation(location, tenantId));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void>deleteWarehouse(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") String tenantId
    ){
        warehouseService.deleteWarehouse(id,tenantId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<WarehouseDTO> updateWarehouse(
            @PathVariable Long id,
            @Valid @RequestBody WarehouseRequest request
    ){
        return ResponseEntity.ok(warehouseService.updateWarehouse(id, request));
    }

    @GetMapping("/available")
    public ResponseEntity<List<WarehouseDTO>>getAvailableWarehouses(
            @RequestHeader("X-Tenant-Id") String tenantId
    ){
        return ResponseEntity.ok(warehouseService.getAvailableWarehouses(tenantId));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateWarehouse(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") String tenantId
    ){
       warehouseService.deactivateWarehouse(id, tenantId);
       return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{warehouseCode}/utilization")
    public ResponseEntity<WarehouseDTO>updateUtilization(
            @PathVariable String warehouseCode,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam Double UtilizationChange
    ){
        return ResponseEntity.ok(
                warehouseService.updateUtilization(warehouseCode,tenantId,UtilizationChange));
    }



}
