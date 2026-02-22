package org.example.tenantmvc.Service;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tenantmvc.Dto.TenantDto;
import org.example.tenantmvc.Entity.Tenant;
import org.example.tenantmvc.Exception.DuplicateTenantException;
import org.example.tenantmvc.Exception.TenantNotFoundException;
import org.example.tenantmvc.Repository.TenantRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.DuplicateFormatFlagsException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final KafkaTemplate<String,String>kafkaTemplate;
    private static final String Tenant_TOPIC = "tenant-events";

    @Transactional
    @CacheEvict(value = "tenants", allEntries = true)
    public TenantDto createTenant(TenantDto tenantDto){
        log.info("Creating tenant with code: {}",tenantDto.getTenantCode());

        if(tenantRepository.existsByTenantCode(tenantDto.getTenantCode())){
            throw new DuplicateTenantException("Tenant with code " + tenantDto.getTenantCode() + "already exists");
        }

        if(tenantRepository.existsByContactEmail(tenantDto.getTenantCode())){
            throw new DuplicateTenantException("Tenant with email " + tenantDto.getContactEmail() + "already exists");

        }

        Tenant tenant = mapToEntity(tenantDto);
        tenant.setDatabaseSchema("tenant_" + tenantDto.getTenantCode());
        tenant.setSubscriptionStartDate(LocalDateTime.now());

        if(tenant.getIsTrial()!=null && tenant.getIsTrial()){
            tenant.setSubscriptionEndDate(LocalDateTime.now().plusDays(30));
        }

        Tenant savedTenant = tenantRepository.save(tenant);

        publishTenantEvent("Tenant_Created",savedTenant);

        log.info("tenant created successfully with Id: {}",savedTenant.getId());
        return mapToDto(savedTenant);

    }

    @Transactional(readOnly = true)
    @Cacheable(value = "tenants", key = "#tenantCode")
    public TenantDto getTenantByCode(String tenantCode){
        log.info("Fetching tenant by Code: {}",tenantCode);

        Tenant tenant = tenantRepository.findByTenantCode(tenantCode)
                .orElseThrow(()-> new TenantNotFoundException("Tenant not found with code :" +tenantCode));
        return mapToDto(tenant);

    }

    @Transactional(readOnly = true)
    public TenantDto getTenantById(Long id) {
        log.info("Fetching tenant by Id: {}", id);
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found with Id:" + id));
        return mapToDto(tenant);
    }

    @Transactional(readOnly = true)
    public List<TenantDto>getAllTenants() {
        log.info("Fetching all tenants");
        return tenantRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

    }

    @Transactional(readOnly = true)
    public List<TenantDto> getTenantsByStatus(Tenant.TenantStatus status) {
        log.info("Fetching tenants by status: {}", status);
        return tenantRepository.findByStatus(status).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "tenants", key = "#tenantCode")
    public TenantDto updateTenant(String tenantCode, TenantDto tenantDTO) {
        log.info("Updating tenant with code: {}", tenantCode);

        Tenant tenant = tenantRepository.findByTenantCode(tenantCode)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found with code: " + tenantCode));

        tenant.setTenantName(tenantDTO.getTenantName());
        tenant.setDescription(tenantDTO.getDescription());
        tenant.setContactEmail(tenantDTO.getContactEmail());
        tenant.setContactPhone(tenantDTO.getContactPhone());
        tenant.setAddress(tenantDTO.getAddress());
        tenant.setMaxUsers(tenantDTO.getMaxUsers());
        tenant.setMaxStorageGb(tenantDTO.getMaxStorageGb());
        tenant.setApiRateLimit(tenantDTO.getApiRateLimit());
        tenant.setMetadata(tenantDTO.getMetadata());

        Tenant updatedTenant = tenantRepository.save(tenant);

        publishTenantEvent("TENANT_UPDATED", updatedTenant);

        log.info("Tenant updated successfully: {}", tenantCode);
        return mapToDto(updatedTenant);

    }

    @Transactional
    @CacheEvict(value = "tenants", key = "#tenantCode")
    public void updateTenantStatus(String tenantCode, Tenant.TenantStatus status){
        log.info("Updating tenant status for code: {} to {}", tenantCode, status);

        Tenant tenant = tenantRepository.findByTenantCode(tenantCode)
                .orElseThrow(()-> new TenantNotFoundException("Tenant not found with code: " + tenantCode));

        tenant.setStatus(status);
        tenantRepository.save(tenant);

        publishTenantEvent("TENANT_STATUS_CHANGED", tenant);
        log.info("Tenant status updated successfully: {}", tenantCode);


    }

    public void deleteTenant(String tenantCode){
        log.info("Deleting tenant with code: {}", tenantCode);

        Tenant tenant = tenantRepository.findByTenantCode(tenantCode)
                .orElseThrow(()-> new TenantNotFoundException("Tenant not found with code: " + tenantCode));


        tenant.setStatus(Tenant.TenantStatus.INACTIVE);
        tenantRepository.save(tenant);

        publishTenantEvent("TENANT_DELETED", tenant);

        log.info("Tenant marked as inactive: {}", tenantCode);
    }

    private void publishTenantEvent(String eventType,Tenant tenant){
        try{
            String message =String.format("{\"eventType\":\"%s\",\"tenantCode\":\"%s\",\"tenantId\":%d,\"status\":\"%s\"}",
                    eventType, tenant.getTenantCode(), tenant.getId(), tenant.getStatus());

            kafkaTemplate.send(Tenant_TOPIC,tenant.getTenantCode(),message);
            log.info("Published event {} for tenant {}",eventType,tenant
                    .getTenantCode());

        }catch (Exception e){
            log.error("Failed to publish tenant event:{}",e.getMessage());
        }
    }

    private TenantDto mapToDto(Tenant tenant){
        TenantDto dto = new TenantDto();
        dto.setId(tenant.getId());
        dto.setTenantCode(tenant.getTenantCode());
        dto.setTenantName(tenant.getTenantName());
        dto.setDescription(tenant.getDescription());
        dto.setContactEmail(tenant.getContactEmail());
        dto.setContactPhone(tenant.getContactPhone());
        dto.setAddress(tenant.getAddress());
        dto.setStatus(tenant.getStatus());
        dto.setSubscriptionTier(tenant.getSubscriptionTier());
        dto.setMaxUsers(tenant.getMaxUsers());
        dto.setMaxStorageGb(tenant.getMaxStorageGb());
        dto.setApiRateLimit(tenant.getApiRateLimit());
        dto.setCreatedAt(tenant.getCreatedAt());
        dto.setUpdatedAt(tenant.getUpdatedAt());
        dto.setSubscriptionStartDate(tenant.getSubscriptionStartDate());
        dto.setSubscriptionEndDate(tenant.getSubscriptionEndDate());
        dto.setIsTrial(tenant.getIsTrial());
        dto.setMetadata(tenant.getMetadata());
        return dto;

    }


    private Tenant mapToEntity(TenantDto dto){
        Tenant tenant = new Tenant();

        tenant.setTenantCode(dto.getTenantCode());
        tenant.setTenantName(dto.getTenantName());
        tenant.setDescription(dto.getDescription());
        tenant.setContactEmail(dto.getContactEmail());
        tenant.setContactPhone(dto.getContactPhone());
        tenant.setAddress(dto.getAddress());
        tenant.setStatus(dto.getStatus() != null ? dto.getStatus() : Tenant.TenantStatus.ACTIVE);
        tenant.setSubscriptionTier(dto.getSubscriptionTier() != null ? dto.getSubscriptionTier() : Tenant.SubscriptionTier.BASIC);
        tenant.setMaxUsers(dto.getMaxUsers() != null ? dto.getMaxUsers() : 10);
        tenant.setMaxStorageGb(dto.getMaxStorageGb() != null ? dto.getMaxStorageGb() : 100);
        tenant.setApiRateLimit(dto.getApiRateLimit() != null ? dto.getApiRateLimit() : 1000);
        tenant.setIsTrial(dto.getIsTrial() != null ? dto.getIsTrial() : false);
        tenant.setMetadata(dto.getMetadata());

        return tenant;

    }


    public void upgradeTenantSubscription(String tenantCode, Tenant.SubscriptionTier tier) {

    }
}
