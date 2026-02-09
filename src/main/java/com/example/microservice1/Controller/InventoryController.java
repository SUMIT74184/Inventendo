package com.example.microservice1.Controller;


import com.example.microservice1.Dto.InventoryRequest;
import com.example.microservice1.Dto.InventoryResponse;
import com.example.microservice1.Service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/inventory")
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryResponse> createInventory(@Valid @RequestBody InventoryRequest request){
        InventoryResponse response = inventoryService.createInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{sku}")
    public ResponseEntity<InventoryResponse> getInventoryBySku(@PathVariable String sku){
        InventoryResponse response = inventoryService.getInventoryBySku(sku);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getAllInventory(){
        List<InventoryResponse> response = inventoryService.getAllInventory();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<List<InventoryResponse>> getInventoryByWareHouse(@PathVariable String warehouseId){
        List<InventoryResponse>response = inventoryService.getInventoryByWarehouse(warehouseId);
        return  ResponseEntity.ok(response);

    }
    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse>> getLowStocksItems(){
        List<InventoryResponse>response = inventoryService.getLowStockItems();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{sku}/quantity")
    public ResponseEntity<InventoryResponse> updateQuantity(
            @PathVariable String sku,
            @RequestBody Map<String,Integer> request
    ){
        InventoryResponse response = inventoryService.updateQuantity(sku,request.get("quantity"));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{sku}/check")
    public ResponseEntity<Map<String,Boolean>> checkAvailability(
            @PathVariable String sku,
            @RequestParam Integer quantity
    ){
        boolean available = inventoryService.checkAvailability(sku,quantity);
        return ResponseEntity.ok(Map.of("available",available));
    }

    @PostMapping("/{sku}/reserve")
    public ResponseEntity<Void> reserveStock(
            @PathVariable String sku,
            @RequestBody Map<String,Integer> request
    ){
        inventoryService.reserveStock(sku,request.get("quantity"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{sku}/release")
    public ResponseEntity<Void> releaseStock(
            @PathVariable String sku,
            @RequestBody Map<String,Integer> request
    ){
        inventoryService.releaseReservedStock(sku,request.get("quantity"));
        return ResponseEntity.ok().build();
    }






}
