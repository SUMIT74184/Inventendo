package org.example.auth.Repository;

import org.example.auth.Entity.OAuth2Provider;
import org.example.auth.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndTenantId(String email,String tenantId);

    boolean existsByEmail(String email);

    List<User> findByTenantId(String tenantId);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.enabled = true")
    List<User> findActiveUsersByTenant(@Param("tenantId") String tenantId);


    long countByTenantId(String tenantId);

    Optional<User> findByOauth2IdAndProvider(String oauth2Id, OAuth2Provider provider);

}
//Added /oauth2/error endpoint for OAuth2 failures
//Added /health endpoint for Kubernetes
//Added /users endpoint stub (admin only)
//Improved /me endpoint
//Better error handling
//Comprehensive JavaDoc comments