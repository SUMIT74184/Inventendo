package org.app.movementmcs.Service;

import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.app.movementmcs.Dto.MovementRequest;
import org.app.movementmcs.Dto.MovementResponse;
import org.app.movementmcs.Dto.StockUpdateEvent;
import org.app.movementmcs.Entity.StockMovement;
import org.app.movementmcs.Repository.StockMovementRepository;
import org.app.movementmcs.context.TenantContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import java.util.UUID;

@Service
@Slf4j
public class MovementService {

    private final StockMovementRepository movementRepository;
    private final KafkaEventProducer kafkaEventProducer;
    private final CacheService cacheService;

    public MovementService(
            StockMovementRepository movementRepository,
            KafkaEventProducer kafkaEventProducer,
            CacheService cacheService) {
        this.movementRepository = movementRepository;
        this.kafkaEventProducer = kafkaEventProducer;
        this.cacheService = cacheService;
    }

    @Transactional
    public MovementResponse createMovement(MovementRequest request) {
        String tenantId = TenantContext.getTenantId();

        var existingMovement = movementRepository.findByTenantIdAndIdempotencyKey(
                tenantId, request.getIdempotencyKey()
        );

        if (existingMovement.isPresent()) {
            log.info("Duplicate request detected: tenantId={}, idempotencyKey={}",
                    tenantId, request.getIdempotencyKey());
            return mapToResponse(existingMovement.get());
        }

        StockMovement movement = StockMovement.builder()
                .productId(request.getProductId())
                .warehouseId(request.getWarehouseId())
                .movementType(request.getMovementType())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .fromLocation(request.getFromLocation())
                .toLocation(request.getToLocation())
                .reason(request.getReason())
                .referenceNumber(request.getReferenceNumber())
                .performedBy(request.getPerformedBy())
                .idempotencyKey(request.getIdempotencyKey())
                .status(StockMovement.MovementStatus.PENDING)
                .build();

        movement.setTenantId(tenantId);

        try {
            StockMovement savedMovement = movementRepository.save(movement);
            savedMovement.setStatus(StockMovement.MovementStatus.COMPLETED);
            savedMovement = movementRepository.save(savedMovement);

            MovementResponse response = mapToResponse(savedMovement);

            cacheService.cacheMovement(tenantId, savedMovement.getId(), response);
            cacheService.invalidateProductMovementsCache(tenantId, request.getProductId());

            StockUpdateEvent event = buildStockUpdateEvent(savedMovement);
            kafkaEventProducer.publishStockUpdateEvent(event);

            if (request.getMovementType() == StockMovement.MovementType.TRANSFER) {
                kafkaEventProducer.publishWarehouseTransferEvent(event);
            }

            log.info("Movement created successfully: tenantId={}, movementId={}, type={}, productId={}",
                    tenantId, savedMovement.getId(), request.getMovementType(), request.getProductId());

            return response;

        } catch (Exception e) {
            log.error("Failed to create movement: tenantId={}, idempotencyKey={}, error={}",
                    tenantId, request.getIdempotencyKey(), e.getMessage(), e);
            throw new RuntimeException("Failed to create movement", e);
        }
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> getMovementsByProduct(Long productId) {
        String tenantId = TenantContext.getTenantId();

        List<MovementResponse> cached = cacheService.getProductMovementsFromCache(tenantId, productId);
        if (cached != null) {
            return cached;
        }

        List<StockMovement> movements = movementRepository
                .findByTenantIdAndProductIdOrderByCreatedAtDesc(tenantId, productId);

        List<MovementResponse> responses = movements.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        cacheService.cacheProductMovements(tenantId, productId, responses);

        return responses;
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> getMovementsByWarehouse(Long warehouseId) {
        String tenantId = TenantContext.getTenantId();

        List<StockMovement> movements = movementRepository
                .findByTenantIdAndWarehouseIdOrderByCreatedAtDesc(tenantId, warehouseId);

        return movements.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> getMovementsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        String tenantId = TenantContext.getTenantId();

        List<StockMovement> movements = movementRepository
                .findMovementsByDateRange(tenantId, startDate, endDate);

        return movements.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private MovementResponse mapToResponse(StockMovement movement) {
        return MovementResponse.builder()
                .id(movement.getId())
                .tenantId(movement.getTenantId())
                .productId(movement.getProductId())
                .warehouseId(movement.getWarehouseId())
                .movementType(movement.getMovementType())
                .quantity(movement.getQuantity())
                .unitPrice(movement.getUnitPrice())
                .fromLocation(movement.getFromLocation())
                .toLocation(movement.getToLocation())
                .reason(movement.getReason())
                .referenceNumber(movement.getReferenceNumber())
                .performedBy(movement.getPerformedBy())
                .idempotencyKey(movement.getIdempotencyKey())
                .status(movement.getStatus())
                .createdAt(movement.getCreatedAt())
                .updatedAt(movement.getUpdatedAt())
                .build();
    }

    private StockUpdateEvent buildStockUpdateEvent(StockMovement movement) {
        return StockUpdateEvent.builder()
                .tenantId(movement.getTenantId())
                .productId(movement.getProductId())
                .warehouseId(movement.getWarehouseId())
                .movementType(movement.getMovementType())
                .quantity(movement.getQuantity())
                .fromLocation(movement.getFromLocation())
                .toLocation(movement.getToLocation())
                .referenceNumber(movement.getReferenceNumber())
                .performedBy(movement.getPerformedBy())
                .timestamp(LocalDateTime.now())
                .eventId(UUID.randomUUID().toString())
                .build();
    }

}