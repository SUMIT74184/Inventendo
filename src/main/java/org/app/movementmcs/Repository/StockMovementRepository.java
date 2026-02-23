package org.app.movementmcs.Repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.app.movementmcs.Entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement,Long> {
    Optional<StockMovement> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);

    List<StockMovement> findByTenantIdAndProductIdOrderByCreatedAtDesc(String tenantId, Long productId);

    List<StockMovement> findByTenantIdAndWarehouseIdOrderByCreatedAtDesc(String tenantId, Long warehouseId);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.tenantId = :tenantId " +
            "AND sm.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY sm.createdAt DESC")
    List<StockMovement> findMovementsByDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT sm FROM StockMovement sm WHERE sm.tenantId = :tenantId " +
            "AND sm.productId = :productId AND sm.warehouseId = :warehouseId " +
            "ORDER BY sm.createdAt DESC")
    List<StockMovement> findByTenantProductAndWarehouse(
            @Param("tenantId") String tenantId,
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId
    );
}
