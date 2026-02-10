package org.example.warehousemcs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.warehousemcs.Dto.WarehouseDTO;
import org.example.warehousemcs.Dto.WarehouseRequest;
import org.example.warehousemcs.model.Warehouse;
import org.example.warehousemcs.repository.WarehouseRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final KafkaTemplate<String,String> KafkaTemplate;
    private final RedisTemplate<String,Object>redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String WAREHOUSE_TOPIC= "warehouse-events";
    private static final String CACHE_PREFIX = "warehouse";

    @Override
    @Transactional
    @CacheEvict(value = "warehouses", allEntries = true)
    public WarehouseDTO createWarehouse(WarehouseRequest req) {
        log.info("Creating warehouse:{}", req.getWarehouseCode());

        if (warehouseRepository.existByWarehouseCodeAndTenantId(
                req.getWarehouseCode(), req.getTenantId())) {
            throw new RuntimeException("Warehouse with code " + req.getWarehouseCode() + "already exists");

        }

        Warehouse warehouse = mapToEntity(req);
        warehouse.setActive(true);
        Warehouse savedWarehouse = warehouseRepository.save(warehouse);


        WarehouseDTO dto = mapToDTO(savedWarehouse);
        cacheWarehouse(dto);
        publishWarehouseEvent("WAREHOUSE_CREATED", dto);

        return dto;
    }
    @Override
    @Transactional
    @CacheEvict(value = "warehouses", allEntries = true)
    public WarehouseDTO updateWarehouse(Long id, WarehouseRequest request){
        log.info("Updating warehouse: {}",id);

        Warehouse warehouse =warehouseRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Warehouse not found"));


        if(!warehouse.getTenantId().equals(request.getTenantId())){
            throw new RuntimeException("Unauthorized access to warehouse");
        }

        updateWarehouseFields(warehouse,request);
        Warehouse updateWarehouse =  warehouseRepository.save(warehouse);

        WarehouseDTO dto = mapToDTO(updateWarehouse);
        cacheWarehouse(dto);
        publishWarehouseEvent("WAREHOUSE_UPDATED",dto);

        return dto;
    }

    @Override
    @Cacheable(value = "warehouses", key = "#id + '-' + #tenantId")
    public WarehouseDTO getWarehouseById(Long id, String tenantId) {
        log.info("Fetching warehouse by ID: {}", id);

        String cacheKey = CACHE_PREFIX + id + ":" + tenantId;
        WarehouseDTO cached = (WarehouseDTO) redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            log.info("Warehouse found in cache");
            return cached;
        }

        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        if (!warehouse.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to warehouse");
        }

        WarehouseDTO dto = mapToDTO(warehouse);
        cacheWarehouse(dto);
        return dto;
    }

    @Override
    @Cacheable(value = "warehouses", key = "#warehouseCode + '-' + #tenantId")
    public WarehouseDTO getWarehouseByCode(String warehouseCode,String tenantId){
        log.info("Fetching warehouse by code: {}", warehouseCode);

        Warehouse warehouse = warehouseRepository.findByWarehouseCodeAndTenantId(warehouseCode,tenantId)
                .orElseThrow(()-> new RuntimeException("Warehouse not found"));

        return mapToDTO(warehouse);
    }

    @Override
    public List<WarehouseDTO>getAllWarehouses(String tenantId){
        log.info("Fetching all warehouses for tenant: {}",tenantId);

        return warehouseRepository.findByTenantId((tenantId))
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<WarehouseDTO> getActiveWarehouses(String tenantId){
        log.info("Fetching active warehouses for tenant: {}",tenantId);

        return warehouseRepository.findByTenantIdAndActive(tenantId,true)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<WarehouseDTO> getWarehouseByStatus(String tenantId, String status){
        log.info("Fetching warehouses by status: {} for tenant:{}",status,tenantId);

        return warehouseRepository.findByTenantIdAndStatus(tenantId,status)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }


    public List<WarehouseDTO>getWarehousesByLocation(String tenantId,String location){

        return warehouseRepository.findByTenantIdAndLocation(tenantId,location)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<WarehouseDTO> getAvailableWarehouses(String tenantId) {
        log.info("Fetching available warehouses for tenant: {}", tenantId);

        return warehouseRepository.findAvailableWarehouses(tenantId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    @CacheEvict(value = "warehouses", allEntries = true)
    public void deleteWarehouse(Long id, String tenantId){
        log.info("Deleting warehouse: {}",id);

        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Warehouse not found"));

        if(!warehouse.getTenantId().equals(tenantId)){
            throw new RuntimeException(("Unauthorized access to warehouse"));
        }

        warehouseRepository.delete(warehouse);
        evictCache(id,tenantId);
        publishWarehouseEvent("WAREHOUSE_DELETED",mapToDTO(warehouse));

    }



    @Override
    @Transactional
    @CacheEvict(value = "warehouses", allEntries = true)
    public void deactivateWarehouse(Long id, String tenantId){
        log.info("Deactivating warehouse: {}", id);

        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Warehouse not found"));

        if(!warehouse.getTenantId().equals(tenantId)){
            throw new RuntimeException("Unauthorized access to warehouse");
        }

        warehouse.setActive(false);
        Warehouse deactivated = warehouseRepository.save(warehouse);
        WarehouseDTO dto = mapToDTO(deactivated);
        evictCache(id,tenantId);
//        publishWarehouseEvent("WAREHOUSE_DEACTIVATED",mapToDTO(warehouse));
        publishWarehouseEvent("WAREHOUSE_DEACTIVATED",dto);

    }

    private void publishWarehouseEvent(String eventType,WarehouseDTO warehouse){

    }

    @Override
    @Transactional
    public WarehouseDTO updateUtilization(String warehouseCode, String tenantId, Double utilizationChange) {
        log.info("Updating utilization for warehouse: {}", warehouseCode);

        Warehouse warehouse = warehouseRepository.findByWarehouseCodeAndTenantId(warehouseCode, tenantId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        Double newUtilization = warehouse.getCurrentUtilization() + utilizationChange;

        if (newUtilization < 0) {
            throw new RuntimeException("Utilization cannot be negative");
        }

        if (newUtilization > warehouse.getCapacity()) {
            throw new RuntimeException("Utilization exceeds warehouse capacity");
        }

        warehouse.setCurrentUtilization(newUtilization);
        Warehouse updatedWarehouse = warehouseRepository.save(warehouse);

        WarehouseDTO dto = mapToDTO(updatedWarehouse);
        cacheWarehouse(dto);
        publishWarehouseEvent("WAREHOUSE_UTILIZATION_UPDATED", dto);

        return dto;
    }

    private void cacheWarehouse(WarehouseDTO warehouse){


    }

    private void evictCache(Long id,String tenantId){

    }

    private void updateWarehouseFields(Warehouse warehouse,WarehouseRequest warehouseRequest){

    }

    private Warehouse mapToEntity(WarehouseRequest request){
        Warehouse warehouse = new Warehouse();
        return warehouse;
    }

    private WarehouseDTO mapToDTO(Warehouse warehouse){
        return WarehouseDTO.builder()
                .id(warehouse.getId())
                .warehouseCode(warehouse.getWarehouseCode())
                .name(warehouse.getName())
                .currentUtilization(warehouse.getCurrentUtilization())
                .active(warehouse.getActive())
                .build();



    }
    private record WarehouseEvent(String eventType,WarehouseDTO warehouse){}














}
