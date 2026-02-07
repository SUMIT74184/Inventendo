package com.example.microservice1.Dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;

//Defines what data client sends in API

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryRequest{

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(max=255,message = "Product name must not exceed 255 characters")
    private String productName;


    private String description;

    @NotNull(message = "Reorder level is required")
    @Min(value = 0, message= "Reorder level must be non-negative")
    private Integer reorderLevel;


    @NotNull(message = "Quantity is required")
    @Min(value=0,message = "Quantity must be non-negative")
    private Integer quantity;

    @NotNull(message = "Max stock level is required")
    @Min(value=1,message = "Max stock level must be positive")
    private Integer maxStockLevel;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0",inclusive = false,message="Unit price must be positive")
    private BigDecimal unitPrice;

    @NotBlank(message = "Warehouse Id is required")
    private String warehouseId;

    private String location;



}
