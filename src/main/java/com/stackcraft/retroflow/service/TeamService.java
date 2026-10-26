package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    /**
     * Creates a team with the supplied member names.
     *
     * @param name the team name
     * @param members the nonempty list of member names
     * @return the saved team with its members initialized
     * @throws RetroflowException if a team business rule is violated
     */
    public Team createTeam(String name, List<String> members) {
        if (name == null || name.isBlank() || name.length() > 200) {
            throw new RetroflowException("Team new: name must contain 1 to 200 characters");
        }
        if (members == null || members.isEmpty()
                || members.stream().anyMatch(member -> member == null || member.isBlank() || member.length() > 200)) {
            throw new RetroflowException("Team new: at least one valid member name is required");
        }
        return teamRepository.save(new Team(name, members));
    }

    /**
     * Retrieves a team and initializes its member names for response mapping.
     *
     * @param id the team identifier
     * @return the team with its members initialized
     * @throws ResourceNotFoundException if the team does not exist
     */
    public Team getTeam(Long id) {
        return teamRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Team", id));
    }
}
