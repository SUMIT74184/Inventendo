package org.example.tenantmvc.Repository;


import org.example.tenantmvc.Entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface TenantRepository extends JpaRepository<Tenant,Long> {
    Optional<Tenant> findByTenantCode(String tenantCode);

    boolean existsByTenantCode(String tenantCode);

    boolean existsByContactEmail(String contactEmail);

    List<Tenant>findByStatus(Tenant.TenantStatus status);


    List<Tenant>findBySubscriptionTier(Tenant.SubscriptionTier tier);

    @Query("SELECT t FROM Tenant t WHERE t.status = 'ACTIVE' AND t.subscriptionEndDate < CURRENT_TIMESTAMP")
    List<Tenant> findExpiredSubscriptions();

    @Query("SELECT t FROM Tenant t WHERE t.isTrial = true AND t.subscriptionEndDate < CURRENT_TIMESTAMP")
    List<Tenant> findExpiredTrials();








}
