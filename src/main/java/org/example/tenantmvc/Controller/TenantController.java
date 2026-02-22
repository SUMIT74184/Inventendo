package org.example.tenantmvc.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tenantmvc.Dto.TenantDto;
import org.example.tenantmvc.Entity.Tenant;
import org.example.tenantmvc.Service.TenantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Slf4j
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<TenantDto>createTenant(@Valid @RequestBody TenantDto tenantDto){
        log.info("Request to create tenant:{}",tenantDto.getTenantCode());
        TenantDto createdTenant = tenantService.createTenant(tenantDto);
        return new ResponseEntity<>(createdTenant, HttpStatus.CREATED);
    }

    @GetMapping("{tenantCode}")
    public ResponseEntity<TenantDto> getTenantByCode(@PathVariable String tenantCode){
        log.info("Request to get tenant by code {}",tenantCode);
        TenantDto tenant = tenantService.getTenantByCode(tenantCode);
        return ResponseEntity.ok(tenant);
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<TenantDto> getTenantById(@PathVariable Long id){
        log.info("Request to get tenant by ID: {}", id);
        TenantDto tenant = tenantService.getTenantById(id);
        return ResponseEntity.ok(tenant);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TenantDto>> getTenantsByStatus(@PathVariable Tenant.TenantStatus status){
        log.info("Request to get tenants by status: {}", status);
        List<TenantDto> tenants = tenantService.getTenantsByStatus(status);
        return ResponseEntity.ok(tenants);
    }


    @PutMapping("/{tenantCode}")
    public ResponseEntity<TenantDto>updateTenant(
           @PathVariable String tenantCode,
           @Valid @RequestBody TenantDto tenantDto
    ){
        log.info("Request to update tenant: {}",tenantCode);
        TenantDto updateTenant =tenantService.updateTenant(tenantCode,tenantDto);
        return ResponseEntity.ok(updateTenant);

    }
    @PatchMapping("/{tenantCode}/status")
    public ResponseEntity<Void> updateTenantStatus(
            @PathVariable String tenantCode,
            @RequestParam Tenant.TenantStatus status
    ){
        log.info("Request to update tenant status: {} to {}",tenantCode,status);
        tenantService.updateTenantStatus(tenantCode,status);
        return ResponseEntity.ok().build();

    }

    @PatchMapping("/{tenantCode}/subscription")
    public ResponseEntity<Void>upgradeTenantSubscription(
            @PathVariable String tenantCode,
            @RequestParam Tenant.SubscriptionTier tier
    ){
        log.info("Request to upgrade tenant subscription: {} to {}",tenantCode,tier);
        tenantService.upgradeTenantSubscription(tenantCode,tier);
        return ResponseEntity.ok().build();
    }


    @DeleteMapping("/{tenantCode}")
    public ResponseEntity<Void> deleteTenant(@PathVariable String tenantCode) {
        log.info("Request to delete tenant: {}", tenantCode);
        tenantService.deleteTenant(tenantCode);
        return ResponseEntity.noContent().build();
    }





}
