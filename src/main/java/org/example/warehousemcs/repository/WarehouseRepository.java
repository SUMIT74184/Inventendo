package org.example.warehousemcs.repository;


import org.example.warehousemcs.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse,Long> {

    Optional<Warehouse> findByWarehouseCodeAndTenantId(String warehouseCode,String tenantId);

    List<Warehouse>findByTenantId(String tenantId);

    List<Warehouse> findByTenantIdAndActive(String tenantId,Boolean active);

    List<Warehouse> findByTenantIdAndStatus(String tenantId,String status);

    List<Warehouse> findByTenantIdAndLocation(String tenantId,String location);

    @Query("SELECT w FROM Warehouse w WHERE w.tenantId = :tenantId AND w.CurrentUtilization < w.capacity")
    List<Warehouse>findAvailableWarehouses(String tenantId);

    boolean existByWarehouseCodeAndTenantId(String warehouseCode,String tenantId);
}
