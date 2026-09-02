package com.teamui.domain.membership;

import com.teamui.domain.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link TeamMembership}. Queries automatically exclude
 * soft-deleted records (where {@code left_at IS NOT NULL}) via
 * {@code @SQLRestriction} on the entity.
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Repository
public interface TeamMembershipRepository extends JpaRepository<TeamMembership, UUID> {

    Page<TeamMembership> findAllByUserId(UUID userId, Pageable pageable);

    Page<TeamMembership> findAllByTeamId(UUID teamId, Pageable pageable);

    List<TeamMembership> findAllByTeamIdAndRoleIn(UUID teamId, List<UserRole> roles);

    Optional<TeamMembership> findByUserIdAndTeamIdAndRole(UUID userId, UUID teamId, UserRole role);
}
