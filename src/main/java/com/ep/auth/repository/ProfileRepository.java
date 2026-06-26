package com.ep.auth.repository;

import com.ep.auth.domain.Profile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    @Query("""
            select p from Profile p
            where ((:tenantId is null and p.tenantId is null) or p.tenantId = :tenantId)
            order by p.name
            """)
    List<Profile> findByTenant(@Param("tenantId") UUID tenantId);

    @Query("""
            select p from Profile p
            where p.name = :name
            and ((:tenantId is null and p.tenantId is null) or p.tenantId = :tenantId)
            """)
    Optional<Profile> findByTenantAndName(@Param("tenantId") UUID tenantId, @Param("name") String name);

    @Modifying
    @Query(value = "insert into user_profiles (user_id, profile_id) values (:userId, :profileId) on conflict do nothing", nativeQuery = true)
    void assignProfileToUser(@Param("userId") UUID userId, @Param("profileId") UUID profileId);

    @Modifying
    @Query(value = "insert into profile_permissions (profile_id, permission_id) values (:profileId, :permissionId) on conflict do nothing", nativeQuery = true)
    void assignPermissionToProfile(@Param("profileId") UUID profileId, @Param("permissionId") UUID permissionId);

    @Query(value = """
            select p.name
            from profiles p
            join user_profiles up on up.profile_id = p.id
            where up.user_id = :userId
            order by p.name
            """, nativeQuery = true)
    List<String> findProfileNamesByUserId(@Param("userId") UUID userId);
}
