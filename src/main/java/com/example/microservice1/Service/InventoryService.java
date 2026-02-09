package com.example.microservice1.Service;

import com.example.microservice1.Dto.InventoryRequest;
import com.example.microservice1.Dto.InventoryResponse;
import com.example.microservice1.Exception.InsufficientStockException;
import com.example.microservice1.Exception.InventoryNotFoundException;
import com.example.microservice1.Repository.InventoryRepository;
import com.example.microservice1.model.Inventory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    private final KafkaTemplate<String,Object> KafkaTemplate;

    @Transactional
    @CacheEvict(value = "inventory", key = "#request.sku")
    public InventoryResponse createInventory(InventoryRequest request){
        Inventory inventory = new Inventory();
        inventory.setSku(request.getSku());
        inventory.setDescription(request.getDescription());
        inventory.setQuantity(request.getQuantity());
        inventory.setReorderLevel(request.getReorderLevel());
        inventory.setMaxStockLevel(request.getMaxStockLevel());
        inventory.setUnitPrice(request.getUnitPrice());
        inventory.setWarehouseId(request.getWarehouseId());
        inventory.setLocation(request.getLocation());

        Inventory saved = inventoryRepository.save(inventory);
        log.info("Created inventory for SKU: {}",saved.getSku());

        //sending the data to the kafka
        KafkaTemplate.send("inventory-created",saved.getSku(),saved);
        return InventoryResponse.fromEntity(saved);

    }


    @Cacheable(value = "inventory", key = "#sku")
    public InventoryResponse getInventoryBySku(String sku){
        Inventory inventory = inventoryRepository.findBySku(sku)
                .orElseThrow(()-> new InventoryNotFoundException("Inventory is not found for SKU:" + sku + "in warehouse"));
        return InventoryResponse.fromEntity(inventory);

    }
    public List<InventoryResponse>getAllInventory(){
        return inventoryRepository.findAll().stream()
                .map(InventoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    //
    public List<InventoryResponse> getInventoryByWarehouse(String warehouseId){
        return inventoryRepository.findByWarehouseId(warehouseId).stream()
                .map(InventoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<InventoryResponse> getLowStockItems(){
        return inventoryRepository.findLowStocksItems().stream()
                .map(InventoryResponse::fromEntity)
                .collect(Collectors.toList());
    }
    @Transactional
    @CacheEvict(value = "inventory" ,key = "#sku")
    public InventoryResponse updateQuantity(String sku, Integer quantity){
        Inventory inventory = inventoryRepository.findBySkuWithLock(sku)
                .orElseThrow(()-> new InventoryNotFoundException("Inventory not found for SKU: " + sku));


        inventory.setQuantity(quantity);
        Inventory updated = inventoryRepository.save(inventory);
        log.info("Updated quantity for SKU: {} to {}",updated.getSku(),quantity);
        KafkaTemplate.send("inventory-updated",sku,updated);

        return InventoryResponse.fromEntity(updated);
    }
    @Transactional
    public boolean checkAvailability(String sku,Integer quantity){
        Inventory inventory = inventoryRepository.findBySku(sku)
                .orElseThrow(()-> new InventoryNotFoundException("Inventory not found for sku "+sku));
        return inventory.getAvailableQuantity()>=quantity;
    }

    @Transactional
    @CacheEvict(value = "inventory", key = "#sku")
    public void reserveStock(String sku, Integer quantity){
        Inventory inventory = inventoryRepository.findBySkuWithLock(sku)
                .orElseThrow(()->new InventoryNotFoundException("Inventory not found for SKU: "+ sku));

        if(inventory.getAvailableQuantity() < quantity){
            throw new InsufficientStockException("Insufficient stock for sku" + sku);

        }
        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventoryRepository.save(inventory);

        log.info("Reserved {} units for SKU: {}",quantity,sku);
        KafkaTemplate.send("inventory-reserved",sku,quantity);
    }

    public void releaseReservedStock(String sku,Integer quantity){
        Inventory inventory = inventoryRepository.findBySkuWithLock(sku)
                .orElseThrow(()->new InventoryNotFoundException("Inventory not found sku : " + sku));

        inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventoryRepository.save(inventory);

        log.info("Released {} units for SKU: {}",quantity,sku);
        KafkaTemplate.send("inventory-released",sku,quantity);
    }


}
