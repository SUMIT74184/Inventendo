package com.example.microservice1.Repository;


import com.example.microservice1.model.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.sku = :sku")
    Optional<Inventory>findBySkuWithLock(@Param("sku")String sku);

    Optional<Inventory>findBySku(String sku);

    List<Inventory> findByWarehouseId(String warehouseId);

    @Query("SELECT i FROM Inventory i WHERE i.quantity <= i.reorderLevel And i.InventoryStatus='ACTIVE'")
    List<Inventory> findLowStocksItems();

    @Query("SELECT i FROM Inventory i WHERE i.warehouseId = :warehouseId AND i.InventoryStatus = 'ACTIVE'")
    List<Inventory> findActiveByWarehouse(@Param("warehouseId") String warehouseId);

    @Modifying
    @Query("UPDATE Inventory i SET i.reservedQuantity = i.reservedQuantity+ :quantity WHERE i.sku= :sku AND i.quantity - i.reservedQuantity>= :quantity")
    int reserveStock(@Param("sku") String sku, @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE Inventory  i SET i.reservedQuantity = i.reservedQuantity - :quantity, i.quantity = i.quantity - :quantity WHERE i.sku=:sku")
    int releaseStock(@Param("sku") String sku, @Param("quantity") Integer quantity);




}
