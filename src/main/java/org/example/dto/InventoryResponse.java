package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private Long productId;
    private String productName;
    private String sku;
    private Integer availableQuantity;
    private BigDecimal price;
    private boolean inStock;
}
