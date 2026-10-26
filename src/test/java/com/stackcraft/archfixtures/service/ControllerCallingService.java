package com.stackcraft.archfixtures.service;

import com.stackcraft.retroflow.controller.TeamController;
import org.springframework.stereotype.Service;

/**
 * Deliberately violates the architecture rules. Lives outside {@code com.stackcraft.retroflow}
 * so Spring's component scan never registers it.
 */
@Service
public class ControllerCallingService {

    private final TeamController teamController;

    public ControllerCallingService(TeamController teamController) {
        this.teamController = teamController;
    }

    public String teamName(Long id) {
        return teamController.getTeam(id).name();
    }
}
