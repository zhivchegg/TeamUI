package com.teamui.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for platform users.
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find an active user by email address.
     *
     * @param email the email to search for
     * @return the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check whether a user exists with the given email.
     *
     * @param email the email to check
     * @return true if a user exists
     */
    boolean existsByEmail(String email);
}
