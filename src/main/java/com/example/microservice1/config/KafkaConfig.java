package com.example.microservice1.config;


import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic inventoryCreatedTopic(){
        return TopicBuilder.name("inventory-created")
                .partitions(3)
                .replicas(1)
                .build();

    }

    @Bean
    public NewTopic inventoryUpdatedTopic(){
        return TopicBuilder.name("inventory-updated")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryReservedTopic() {
        return TopicBuilder.name("inventory-reserved")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryReleasedTopic() {
        return TopicBuilder.name("inventory-released")
                .partitions(3)
                .replicas(1)
                .build();
    }

}
