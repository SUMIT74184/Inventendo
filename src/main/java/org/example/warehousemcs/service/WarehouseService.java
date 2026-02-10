package org.example.warehousemcs.service;

import org.example.warehousemcs.Dto.WarehouseDTO;
import org.example.warehousemcs.Dto.WarehouseRequest;
import org.springframework.stereotype.Service;

import java.util.List;


public interface WarehouseService {

    WarehouseDTO createWarehouse(WarehouseRequest request);

    WarehouseDTO updateWarehouse(Long id, WarehouseRequest request);

    WarehouseDTO getWarehouseById(Long id, String tenantId);

    WarehouseDTO getWarehouseByCode(String warehouseCode,String tenantId);

    List<WarehouseDTO>getAllWarehouses(String tenantId);

    List<WarehouseDTO>getActiveWarehouses(String tenantId);

//    List<WarehouseDTO> getWarehousesByStatus(String tenantId,String status);

    List<WarehouseDTO> getWarehousesByLocation(String tenantId,String location);

    List<WarehouseDTO> getWarehouseByStatus(String tenantId, String status);

    List<WarehouseDTO>getAvailableWarehouses(String tenantId);

    void deleteWarehouse(Long id, String tenantId);

    WarehouseDTO updateUtilization(String warehouseCode,String tenantId,Double utilizationChange);

    void deactivateWarehouse(Long id, String tenantId);
}
