package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TeamServiceTest {

    private static final long MISSING_TEAM_ID = 999_999L;
    private static final List<String> MEMBERS = List.of("Alice", "Bob");

    private final TeamService teams;
    private final TeamRepository teamRepository;

    @Autowired
    TeamServiceTest(TeamService teams, TeamRepository teamRepository) {
        this.teams = teams;
        this.teamRepository = teamRepository;
    }

    @Test
    @DisplayName("test-matrix L31: a team with a name and members is created")
    void createTeam_whenNameAndMembersAreValid_persistsTeamWithMembers() {
        // Arrange
        var teamName = "Platform";

        // Act
        var created = teams.createTeam(teamName, MEMBERS);

        // Assert
        var stored = teamRepository.findById(created.getId()).orElseThrow();
        assertThat(stored.getName()).isEqualTo(teamName);
        assertThat(stored.getMembers()).containsExactlyElementsOf(MEMBERS);
    }

    @Test
    @DisplayName("test-matrix L39: an existing team can be retrieved")
    void getTeam_whenTeamExists_returnsThatTeam() {
        // Arrange
        var created = teams.createTeam("Platform", MEMBERS);

        // Act
        var found = teams.getTeam(created.getId());

        // Assert
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getMembers()).containsExactlyElementsOf(MEMBERS);
    }

    @Test
    @DisplayName("test-matrix L40: a missing team is reported as not found")
    void getTeam_whenTeamDoesNotExist_throwsNotFoundInStandardFormat() {
        // Act & Assert
        assertThatThrownBy(() -> teams.getTeam(MISSING_TEAM_ID))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Team " + MISSING_TEAM_ID + ": not found");
    }
}
