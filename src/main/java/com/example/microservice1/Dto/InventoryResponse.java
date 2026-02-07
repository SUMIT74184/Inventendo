package com.example.microservice1.Dto;

import com.example.microservice1.model.Inventory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryResponse {
    private Long id;
    private String sku;
    private String productName;
    private Integer quantity;
    private String Description;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Integer reorderLevel;
    private Integer maxStockLevel;
    private BigDecimal unitPrice;
    private String warehouseId;
    private String location;
    private String status;
    private boolean Lowstock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InventoryResponse fromEntity(Inventory inventory){
        InventoryResponse response =  new InventoryResponse();
        response.setId(inventory.getId());
        response.setSku(inventory.getSku());
        response.setDescription(inventory.getDescription());
        response.setProductName(inventory.getProductName());
        response.setQuantity(inventory.getQuantity());
        response.setReservedQuantity(inventory.getReservedQuantity());
        response.setAvailableQuantity(inventory.getAvailableQuantity());
        response.setReorderLevel(inventory.getReorderLevel());
        response.setMaxStockLevel(inventory.getMaxStockLevel());
        response.setUnitPrice(inventory.getUnitPrice());
        response.setLocation(inventory.getLocation());
        response.setLowstock(inventory.isLowStock());
        response.setCreatedAt(inventory.getCreatedAt());
        response.setUpdatedAt(inventory.getUpdatedAt());
        return response;

    }




}
