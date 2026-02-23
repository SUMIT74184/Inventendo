package org.app.movementmcs.Service;

import lombok.extern.slf4j.Slf4j;
import org.app.movementmcs.Dto.StockUpdateEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class KafkaEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.stock-updated}")
    private String stockUpdatedTopic;

    @Value("${kafka.topics.warehouse-transfer}")
    private String warehouseTransferTopic;

    public KafkaEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishStockUpdateEvent(StockUpdateEvent event) {
        String key = event.getTenantId() + ":" + event.getProductId();

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(stockUpdatedTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Stock update event published successfully: tenantId={}, productId={}, eventId={}",
                        event.getTenantId(), event.getProductId(), event.getEventId());
            } else {
                log.error("Failed to publish stock update event: tenantId={}, productId={}, error={}",
                        event.getTenantId(), event.getProductId(), ex.getMessage(), ex);
            }
        });
    }

    public void publishWarehouseTransferEvent(StockUpdateEvent event) {
        String key = event.getTenantId() + ":" + event.getWarehouseId();

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(warehouseTransferTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Warehouse transfer event published: tenantId={}, warehouseId={}, eventId={}",
                        event.getTenantId(), event.getWarehouseId(), event.getEventId());
            } else {
                log.error("Failed to publish warehouse transfer event: tenantId={}, warehouseId={}, error={}",
                        event.getTenantId(), event.getWarehouseId(), ex.getMessage(), ex);
            }
        });
    }
}
