package org.app.movementmcs.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.app.movementmcs.Entity.StockMovement;

import java.math.BigDecimal;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotNull(message = "Movement type is required")
    private StockMovement.MovementType movementType;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    private BigDecimal unitPrice;

    private String fromLocation;

    private String toLocation;

    private String reason;

    private String referenceNumber;

    @NotNull(message = "Idempotency key is required")
    private String idempotencyKey;

    private String performedBy;

}
