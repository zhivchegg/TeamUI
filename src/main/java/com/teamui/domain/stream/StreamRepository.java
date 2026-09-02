package com.teamui.domain.stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for organisational streams.
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Repository
public interface StreamRepository extends JpaRepository<Stream, UUID> {
}
