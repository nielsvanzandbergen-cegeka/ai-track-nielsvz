package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.NeedsImprovementFeedback;
import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.entity.WentWellFeedback;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.FeedbackItemRepository;
import com.stackcraft.retroflow.repository.RetrospectiveRepository;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class RetrospectiveService {

    private final TeamRepository teamRepository;
    private final RetrospectiveRepository retrospectiveRepository;
    private final FeedbackItemRepository feedbackItemRepository;

    public RetrospectiveService(TeamRepository teamRepository,
                                RetrospectiveRepository retrospectiveRepository,
                                FeedbackItemRepository feedbackItemRepository) {
        this.teamRepository = teamRepository;
        this.retrospectiveRepository = retrospectiveRepository;
        this.feedbackItemRepository = feedbackItemRepository;
    }

    /**
     * Creates an open retrospective for a team on an explicit meeting date.
     *
     * @param teamId the team identifier
     * @param title the retrospective title
     * @param date the meeting date
     * @return the saved retrospective
     * @throws ResourceNotFoundException if the team does not exist
     * @throws RetroflowException if the team already has an open retrospective
     */
    @Transactional
    public Retrospective createRetrospective(Long teamId, String title, LocalDate date) {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));
        if (title == null || title.isBlank() || title.length() > 200 || date == null) {
            throw new RetroflowException("Team " + teamId + ": retrospective title and date are required");
        }
        if (retrospectiveRepository.existsByTeamIdAndStatus(teamId, RetrospectiveStatus.OPEN)) {
            throw new RetroflowException("Team " + teamId + ": already has an open retrospective");
        }
        return retrospectiveRepository.save(new Retrospective(team, title, date));
    }

    /**
     * Uses the current date when the caller does not supply a meeting date.
     *
     * @param teamId the team identifier
     * @param title the retrospective title
     * @return the saved retrospective
     * @throws ResourceNotFoundException if the team does not exist
     * @throws RetroflowException if the team already has an open retrospective
     */
    @Transactional
    public Retrospective createRetrospective(Long teamId, String title) {
        return createRetrospective(teamId, title, LocalDate.now());
    }

    /**
     * Lists a team's retrospectives, distinguishing a missing team from an empty history.
     *
     * @param teamId the team identifier
     * @return the retrospectives, or an empty list for an existing team without history
     * @throws ResourceNotFoundException if the team does not exist
     */
    public List<Retrospective> listRetrospectives(Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new ResourceNotFoundException("Team", teamId);
        }
        return retrospectiveRepository.findAllByTeamId(teamId);
    }

    /**
     * Closes a retrospective without allowing it to be reopened.
     *
     * @param id the retrospective identifier
     * @throws ResourceNotFoundException if the retrospective does not exist
     * @throws RetroflowException if the requested transition violates the agreed closure policy
     */
    @Transactional
    public void closeRetrospective(Long id) {
        Retrospective retrospective = findRetrospective(id);
        retrospective.close();
        retrospectiveRepository.save(retrospective);
    }

    /**
     * Preserves the inherited feedback test contract; no HTTP operation is exposed here.
     *
     * @param retrospectiveId the retrospective identifier
     * @param content the feedback text
     * @param type the feedback type
     * @param submittedBy the submitting member name
     * @return the saved feedback
     * @throws ResourceNotFoundException if the retrospective does not exist
     * @throws RetroflowException if feedback cannot be added
     */
    @Transactional
    public FeedbackItem addFeedbackItem(Long retrospectiveId, String content, String type, String submittedBy) {
        Retrospective retrospective = findRetrospective(retrospectiveId);
        requireOpen(retrospective);
        FeedbackType feedbackType = parseFeedbackType(retrospectiveId, type);
        FeedbackItem item = switch (feedbackType) {
            case WENT_WELL -> new WentWellFeedback(retrospective, content, submittedBy);
            case NEEDS_IMPROVEMENT -> new NeedsImprovementFeedback(retrospective, content, submittedBy);
            case ACTION_ITEM -> throw new RetroflowException(
                "Retrospective " + retrospectiveId + ": use addActionItem to provide action priority");
        };
        return feedbackItemRepository.save(item);
    }

    /**
     * Preserves the inherited action creation test contract.
     *
     * @param retrospectiveId the retrospective identifier
     * @param content the action text
     * @param priority the action priority
     * @param submittedBy the submitting member name
     * @return the saved action
     * @throws ResourceNotFoundException if the retrospective does not exist
     * @throws RetroflowException if the action cannot be added
     */
    @Transactional
    public ActionItem addActionItem(Long retrospectiveId, String content, String priority, String submittedBy) {
        Retrospective retrospective = findRetrospective(retrospectiveId);
        requireOpen(retrospective);
        Priority actionPriority = parsePriority(retrospectiveId, priority);
        return feedbackItemRepository.save(new ActionItem(retrospective, content, actionPriority, submittedBy));
    }

    /**
     * Preserves the inherited action completion test contract.
     *
     * @param id the action identifier
     * @throws ResourceNotFoundException if the action does not exist
     * @throws RetroflowException if completion violates the agreed policy
     */
    @Transactional
    public void completeActionItem(Long id) {
        ActionItem actionItem = findActionItem(id);
        actionItem.complete();
        feedbackItemRepository.save(actionItem);
    }

    /**
     * Preserves the inherited test contract for rejecting reversal of completion.
     *
     * @param id the action identifier
     * @throws ResourceNotFoundException if the action does not exist
     * @throws RetroflowException if the action is already completed
     */
    public void uncompleteActionItem(Long id) {
        ActionItem actionItem = findActionItem(id);
        if (actionItem.isCompleted()) {
            throw new RetroflowException("ActionItem " + id + ": already completed, cannot be undone");
        }
    }

    private Retrospective findRetrospective(Long id) {
        return retrospectiveRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Retrospective", id));
    }

    private ActionItem findActionItem(Long id) {
        FeedbackItem item = feedbackItemRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ActionItem", id));
        if (item instanceof ActionItem actionItem) {
            return actionItem;
        }
        throw new ResourceNotFoundException("ActionItem", id);
    }

    private void requireOpen(Retrospective retrospective) {
        if (retrospective.getStatus() != RetrospectiveStatus.OPEN) {
            throw new RetroflowException(
                "Retrospective " + retrospective.getId() + ": cannot add feedback to a closed retrospective");
        }
    }

    private FeedbackType parseFeedbackType(Long retrospectiveId, String type) {
        try {
            return FeedbackType.valueOf(type);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new RetroflowException("Retrospective " + retrospectiveId + ": unsupported feedback type");
        }
    }

    private Priority parsePriority(Long retrospectiveId, String priority) {
        try {
            return Priority.valueOf(priority);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new RetroflowException("Retrospective " + retrospectiveId + ": unsupported action priority");
        }
    }
}
