package org.example.tenantmvc.Dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.tenantmvc.Entity.Tenant;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantDto {

    private Long id;

    @NotBlank(message = "Tenant code is required")
    @Size(min = 3, max = 50, message = "Tenant code must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Tenant code must contain only lowercase letters, numbers, and hyphens")
    private String tenantCode;

    @NotBlank(message = "Tenant name is required")
    @Size(max = 255, message = "Tenant name must not exceed 255 characters")
    private String tenantName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Invalid email format")
    private String contactEmail;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number")
    private String contactPhone;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    private Tenant.TenantStatus status;

    private Tenant.SubscriptionTier subscriptionTier;

    @Min(value = 1, message = "Max users must be at least 1")
    private Integer maxUsers;

    @Min(value = 1, message = "Max storage must be at least 1 GB")
    private Integer maxStorageGb;

    @Min(value = 100, message = "API rate limit must be at least 100")
    private Integer apiRateLimit;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime subscriptionStartDate;

    private LocalDateTime subscriptionEndDate;

    private Boolean isTrial;

    private String metadata;
}
