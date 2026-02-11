package org.example.service;

import org.example.dto.InventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name="inventory-service", url = "${inventory.service.url}")
public interface InventoryClient {

    @GetMapping("/api/inventory/product/{productId}")
    InventoryResponse getInventory(@PathVariable Long productId);

    @PutMapping("/api/inventory/reserve/{productId}")
    boolean reserveInventory(@PathVariable Long productId, @RequestParam Integer quantity);

    @PutMapping("/api/inventory/release/{productId}")
    void releaseInventory(@PathVariable Long productId, @RequestParam Integer quantity);

}
