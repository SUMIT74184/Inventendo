package org.example.auth.config;


import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

/**
 * KafkaConfig: Manually wires up Kafka Producer and Consumer beans.

 * Why not just use application.yml auto-config?
 * Auto-config works for simple cases, but explicit config gives you:
 * - Type safety (no typos in property names)
 * - Ability to create multiple producers/consumers with different settings
 * - Easier to understand what's configured when reading the code

 * PRODUCER: Publishes messages TO Kafka topics.
 * Auth Service publishes: "user.registered", "user.login" events
 * → Other services (Email Service, Audit Service) consume these

 * CONSUMER: Reads messages FROM Kafka topics.
 * Auth Service consumes: "tenant.created" events
 * → So it can set up default admin user for new tenants
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ─────────────────────────────────────────
    // PRODUCER CONFIGURATION
    // ─────────────────────────────────────────

    /**
     * ProducerFactory: Creates the Kafka Producer instance.

     * Key configs:
     * - BOOTSTRAP_SERVERS: Kafka broker address (localhost:9092 locally, service name in K8s)
     * - KEY_SERIALIZER: How to convert message KEY to bytes — we use String keys (e.g., userId)
     * - VALUE_SERIALIZER: How to convert message VALUE to bytes — we send JSON as String
     * - ACKS_CONFIG "all": Message is confirmed only when ALL replicas acknowledge it
     *   (most reliable option — use "1" if you prefer speed over reliability)
     * - RETRIES: Retry 3 times before giving up if broker is temporarily unreachable
     * - ENABLE_IDEMPOTENCE: Ensures the SAME message isn't published twice on retry
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        // Batch messages together for efficiency (16KB batches)
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        // Wait up to 1ms for more messages before sending a batch
        config.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * KafkaTemplate: The class your @Service uses to actually SEND messages.
     * Wraps ProducerFactory — you @Autowire KafkaTemplate and call .send(topic, key, value)
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ─────────────────────────────────────────
    // CONSUMER CONFIGURATION
    // ─────────────────────────────────────────

    /**
     * ConsumerFactory: Creates Kafka Consumer instances.

     * Key configs:
     * - GROUP_ID: Consumer group name. Kafka delivers each message to ONE consumer per group.
     *   If you run 3 instances of auth-service, all join "auth-service-group"
     *   and Kafka splits the partitions between them automatically (load balancing!).
     * - AUTO_OFFSET_RESET "earliest": If this consumer hasn't read a topic before,
     *   start from the BEGINNING of the topic (don't miss old messages).
     *   Use "latest" if you only care about new messages.
     * - ENABLE_AUTO_COMMIT false: Don't auto-commit offsets.
     *   We manually commit AFTER successfully processing the message.
     *   If processing fails, message is retried (no data loss).
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "auth-service-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * ConcurrentKafkaListenerContainerFactory: The factory that creates listener containers.
     * Methods annotated with @KafkaListener use this factory under the hood.

     * setConcurrency(3): Run 3 consumer threads in parallel.
     * Good when your topic has 3 partitions — each thread handles one partition.
     * Match this number to your Kafka topic's partition count.

     * AckMode.MANUAL: We manually call acknowledgment.acknowledge() after processing.
     * Ensures we don't lose messages if our service crashes mid-processing.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(
                org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL
        );
        return factory;
    }
}




