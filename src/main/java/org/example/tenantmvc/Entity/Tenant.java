package org.example.tenantmvc.Entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

//import java.io.Serializable;
import java.time.LocalDateTime;


@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String tenantCode;

    @Column(nullable = false, length =  255)
    private String tenantName;

    @Column(nullable = false,length = 100)
    private String contactEmail;

    @Column(length = 500)
    private String address;

    @Column(length = 10)
    private String contactPhone;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionTier subscriptionTier = SubscriptionTier.BASIC;

    @Column(name = "max_users")
    private Integer maxUsers = 10;

    @Column(name = "max_Storage_gb")
    private Integer maxStorageGb = 100;

    @Column(name = "api_rate_limit")
    private Integer apiRateLimit = 1000;

    @Column(name = "database_schema", length = 100)
    private String databaseSchema;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "subscription_start_date")
    private LocalDateTime subscriptionStartDate;

    @Column(name = "subscription_end_date")
    private LocalDateTime subscriptionEndDate;

    @Column(name = "is_trial")
    private Boolean isTrial = false;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    public enum TenantStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        PENDING_ACTIVATION
    }

    public enum SubscriptionTier {
        BASIC,
        STANDARD,
        PREMIUM,
        ENTERPRISE
    }






}
