package org.example.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.auth.Entity.Role;
import org.example.auth.Entity.User;
import org.example.auth.Repository.UserRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/* *
 * AuthKafkaListener: Listens to Kafka topics and reacts to events.
 *
 * @Component: Registers as a Spring Bean so @KafkaListener methods are picked up.
 *
 * This is EVENT-DRIVEN architecture:
 * Instead of Tenant Service calling Auth Service's REST API directly (tight coupling),
 * Tenant Service just fires a "tenant.created" Kafka event.
 * Auth Service listens and creates the admin user.
 * Services are completely decoupled — neither knows about the other!
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuthKafkaListener {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /* *
     * @KafkaListener: Marks this method as a Kafka consumer.
     *
     * topics: Which topic(s) to listen to.
     * groupId: Consumer group — must match what's in KafkaConfig.
     * containerFactory: Which factory bean to use (the one we defined in KafkaConfig).
     *
     * ConsumerRecord<String, String>: The raw Kafka message.
     *   - key()   = message key (we use tenantId as key for partitioning)
     *   - value() = message body (JSON string)
     *
     * Acknowledgment: Manual ack object. Call ack.acknowledge() ONLY after
     * successful processing. If we crash before ack, Kafka will redeliver the message.
     */
    @KafkaListener(
            topics = "tenant.created",
            groupId = "auth-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTenantCreated(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Received tenant.created event: key={}, value={}", record.key(), record.value());

        try {
            // In production: use ObjectMapper to parse the JSON properly
            // For now, we demonstrate the pattern with a simple approach
            String tenantId = record.key(); // We send tenantId as the Kafka message key

            // Check if admin already exists for this tenant (idempotency — safe to retry)
            boolean adminExists = userRepository.existsByEmail("admin@tenant-" + tenantId + ".local");
            if (adminExists) {
                log.info("Admin already exists for tenant: {}", tenantId);
                ack.acknowledge();
                return;
            }

            // Auto-create a TENANT_ADMIN user when a new tenant is created
            // They can change password on first login
            User tenantAdmin = User.builder()
                    .email("admin@tenant-" + tenantId + ".local")
                    .password(passwordEncoder.encode("ChangeMe@123"))
                    .firstName("Tenant")
                    .lastName("Admin")
                    .tenantId(tenantId)
                    .roles(Set.of(Role.TENANT_ADMIN))
                    .enabled(true)
                    .accountNonLocked(true)
                    .build();

            userRepository.save(tenantAdmin);
            log.info("Default admin created for new tenant: {}", tenantId);

            // Acknowledge AFTER successful processing — now Kafka won't redeliver
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing tenant.created event: {}", e.getMessage());
            // Do NOT acknowledge — Kafka will retry after a delay
            // In production: add a Dead Letter Topic (DLT) for messages that keep failing
        }
    }

    /**
     * Listen for password reset requests from other services or API gateway.
     * Demonstrates listening to multiple topics.
     */
    @KafkaListener(
            topics = "auth.password-reset-requested",
            groupId = "auth-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePasswordResetRequest(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Password reset requested for user key: {}", record.key());
        // TODO: Implement password reset logic (send email with reset link)
        // For now just acknowledge
        ack.acknowledge();
    }
}




