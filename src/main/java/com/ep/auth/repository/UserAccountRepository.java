package com.ep.auth.repository;

import com.ep.auth.domain.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    @Query("""
            select u from UserAccount u
            where lower(u.email) = lower(:email)
            and ((:tenantId is null and u.tenantId is null) or u.tenantId = :tenantId)
            """)
    Optional<UserAccount> findByTenantAndEmail(@Param("tenantId") UUID tenantId, @Param("email") String email);
}
