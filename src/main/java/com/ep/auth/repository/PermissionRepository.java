package com.ep.auth.repository;

import com.ep.auth.domain.Permission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    Optional<Permission> findByCode(String code);

    @Query(value = """
            select distinct perm.code
            from permissions perm
            join profile_permissions pp on pp.permission_id = perm.id
            join user_profiles up on up.profile_id = pp.profile_id
            where up.user_id = :userId
            order by perm.code
            """, nativeQuery = true)
    List<String> findPermissionCodesByUserId(@Param("userId") UUID userId);
}
