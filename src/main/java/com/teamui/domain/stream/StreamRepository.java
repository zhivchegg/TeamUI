package com.teamui.domain.stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for organisational streams.
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Repository
public interface StreamRepository extends JpaRepository<Stream, UUID> {

    /** Streams this user leads as stream lead. */
    List<Stream> findAllByStreamLeadId(UUID userId);

    /** Streams this user leads as IT lead. */
    List<Stream> findAllByStreamItLeadId(UUID userId);
}
