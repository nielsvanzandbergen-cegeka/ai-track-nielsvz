package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * STUB — left behind by the previous vendor.
 *
 * Extends JpaRepository for basic CRUD out of the box; add derived query
 * methods here as the service layer needs them (e.g. finding the current
 * OPEN retrospective for a team).
 */
public interface RetrospectiveRepository extends JpaRepository<Retrospective, Long> {
    boolean existsByTeamIdAndStatus(Long teamId, RetrospectiveStatus status);

    List<Retrospective> findAllByTeamId(Long teamId);
}
