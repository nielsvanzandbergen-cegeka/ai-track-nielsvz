package com.stackcraft.archfixtures.controller;

import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Deliberately violates the architecture rules. Lives outside {@code com.stackcraft.retroflow}
 * so Spring's component scan never registers it.
 */
@RestController
public class RepositoryLeakingController {

    private final TeamRepository teamRepository;

    public RepositoryLeakingController(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public Team getTeam(Long id) {
        return teamRepository.findById(id).orElseThrow();
    }

    public ResponseEntity<List<Team>> listTeams() {
        return ResponseEntity.ok(teamRepository.findAll());
    }
}
