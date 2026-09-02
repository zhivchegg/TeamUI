package com.teamui.domain.invite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for invitation tokens.
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Repository
public interface InviteRepository extends JpaRepository<Invite, UUID> {

    Optional<Invite> findByToken(String token);

    boolean existsByToken(String token);
}
