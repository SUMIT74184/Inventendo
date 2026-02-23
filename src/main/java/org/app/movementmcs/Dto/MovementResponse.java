package org.app.movementmcs.Dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.app.movementmcs.Entity.StockMovement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementResponse {
    private Long id;
    private String tenantId;
    private Long productId;
    private Long warehouseId;
    private StockMovement.MovementType movementType;
    private Integer quantity;
    private BigDecimal unitPrice;
    private String fromLocation;
    private String toLocation;
    private String reason;
    private String referenceNumber;
    private String performedBy;
    private String idempotencyKey;
    private StockMovement.MovementStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
