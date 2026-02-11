package org.example.service;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object>KafkaTemplate;

    public void publishOrderCreatedEvent(Long orderId,String orderNumber){
        try{
            OrderEvent event = OrderEvent.builder()
                    .orderId(orderId)
                    .orderNumber(orderNumber)
                    .eventType("ORDER_CREATED")
                    .build();

            KafkaTemplate.send("order-events", orderNumber,event);
            log.info("Published ORDER_CREATED event for order: {}",orderNumber);
        }catch (Exception e){
            log.error("Error publishing order created event ", e);

        }
    }

    public void publishOrderStatusChangedEvent(Long orderId, String orderNumber, String status){
        try{
            OrderEvent event = OrderEvent.builder()
                    .orderId(orderId)
                    .orderNumber(orderNumber)
                    .eventType("ORDER_STATUS_CHANGED")
                    .status(status)
                    .build();

            KafkaTemplate.send("order-event",orderNumber,event);
            log.info("Published ORDER_STATUS_CHANGED event for order: {}",orderNumber);
        }catch (Exception e){
            log.error("Error publishing order status changed event",e);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderEvent{
        private Long orderId;
        private String orderNumber;
        private String eventType;
        private String status;
    }


}
