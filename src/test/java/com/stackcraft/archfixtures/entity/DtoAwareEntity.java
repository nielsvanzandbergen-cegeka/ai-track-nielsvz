package com.stackcraft.archfixtures.entity;

import com.stackcraft.retroflow.dto.TeamResponse;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Deliberately violates the architecture rules. Lives outside {@code com.stackcraft.retroflow}
 * so JPA entity scanning never picks it up.
 */
@Entity
public class DtoAwareEntity {

    @Id
    private Long id;

    public TeamResponse toResponse() {
        return new TeamResponse(id, "name", java.util.List.of());
    }
}
