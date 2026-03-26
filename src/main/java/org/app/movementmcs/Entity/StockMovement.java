package org.app.movementmcs.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "stock_movements", indexes = {
        @Index(name = "idx_tenant_product", columnList = "tenant_id, product_id"),
        @Index(name = "idx_tenant_created", columnList = "tenant_id, created_at"),
        @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private MovementType movementType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "from_location")
    private String fromLocation;

    @Column(name = "to_location")
    private String toLocation;

    @Column(length = 500)
    private String reason;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(name = "performed_by", nullable = false)
    private String performedBy;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementStatus status;

    public enum MovementType {
        IN,
        OUT,
        TRANSFER,
        ADJUSTMENT,
        DAMAGED,
        RETURNED
    }

    public enum MovementStatus {
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}