package org.app.movementmcs.Dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.app.movementmcs.Entity.StockMovement;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockUpdateEvent {
    private String tenantId;
    private Long productId;
    private Long warehouseId;
    private StockMovement.MovementType movementType;
    private Integer quantity;
    private String fromLocation;
    private String toLocation;
    private String referenceNumber;
    private String performedBy;
    private LocalDateTime timestamp;
    private String eventId;
}
