package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.FeedbackItemRepository;
import com.stackcraft.retroflow.repository.RetrospectiveRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class RetrospectiveServiceTest {

    private static final long MISSING_ID = 999_999L;
    private static final LocalDate MEETING_DATE = LocalDate.of(2026, 10, 26);
    private static final String SUBMITTER = "Alice";

    private final TeamService teams;
    private final RetrospectiveService retrospectives;
    private final RetrospectiveRepository retrospectiveRepository;
    private final FeedbackItemRepository feedbackRepository;

    @Autowired
    RetrospectiveServiceTest(TeamService teams, RetrospectiveService retrospectives,
                             RetrospectiveRepository retrospectiveRepository,
                             FeedbackItemRepository feedbackRepository) {
        this.teams = teams;
        this.retrospectives = retrospectives;
        this.retrospectiveRepository = retrospectiveRepository;
        this.feedbackRepository = feedbackRepository;
    }

    @Test
    @DisplayName("test-matrix L54, L65: a retrospective is created OPEN and belongs to its team")
    void createRetrospective_whenTeamHasNoOpenRetrospective_createsOpenRetrospectiveForTeam() {
        // Arrange
        var team = newTeam();

        // Act
        var retro = retrospectives.createRetrospective(team.getId(), "Sprint 1", MEETING_DATE);

        // Assert
        var stored = retrospectiveRepository.findById(retro.getId()).orElseThrow();
        assertThat(stored.getTeam().getId()).isEqualTo(team.getId());
        assertThat(stored.getStatus()).isEqualTo(RetrospectiveStatus.OPEN);
    }

    @Test
    @DisplayName("test-matrix L55: creating a retrospective for a missing team is not found")
    void createRetrospective_whenTeamDoesNotExist_throwsNotFoundAndCreatesNothing() {
        // Arrange
        var countBefore = retrospectiveRepository.count();

        // Act & Assert
        assertThatThrownBy(() -> retrospectives.createRetrospective(MISSING_ID, "Orphan", MEETING_DATE))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Team " + MISSING_ID + ": not found");
        assertThat(retrospectiveRepository.count()).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("test-matrix L66: a second OPEN retrospective is rejected and the first stays unchanged")
    void createRetrospective_whenTeamAlreadyHasOpenRetrospective_rejectsAndKeepsExistingOpen() {
        // Arrange
        var team = newTeam();
        var existing = retrospectives.createRetrospective(team.getId(), "Sprint 1", MEETING_DATE);

        // Act & Assert
        assertThatThrownBy(() -> retrospectives.createRetrospective(team.getId(), "Sprint 2", MEETING_DATE))
            .isInstanceOf(RetroflowException.class);
        assertThat(retrospectiveRepository.findAllByTeamId(team.getId())).containsExactly(existing);
        assertThat(existing.getStatus()).isEqualTo(RetrospectiveStatus.OPEN);
    }

    @Test
    @DisplayName("test-matrix L67: closed history does not prevent a new retrospective")
    void createRetrospective_whenOnlyClosedRetrospectivesExist_createsNewOpenRetrospective() {
        // Arrange
        var team = newTeam();
        var previous = retrospectives.createRetrospective(team.getId(), "Sprint 1", MEETING_DATE);
        retrospectives.closeRetrospective(previous.getId());

        // Act
        var next = retrospectives.createRetrospective(team.getId(), "Sprint 2", MEETING_DATE.plusDays(14));

        // Assert
        assertThat(next.getStatus()).isEqualTo(RetrospectiveStatus.OPEN);
        assertThat(retrospectiveRepository.findAllByTeamId(team.getId())).hasSize(2);
    }

    @Test
    @DisplayName("test-matrix L68: another team's OPEN retrospective does not block creation")
    void createRetrospective_whenOnlyAnotherTeamHasOpenRetrospective_createsRetrospective() {
        // Arrange
        var otherTeam = newTeam();
        retrospectives.createRetrospective(otherTeam.getId(), "Other", MEETING_DATE);
        var targetTeam = newTeam();

        // Act
        var created = retrospectives.createRetrospective(targetTeam.getId(), "Target", MEETING_DATE);

        // Assert
        assertThat(created.getTeam().getId()).isEqualTo(targetTeam.getId());
        assertThat(created.getStatus()).isEqualTo(RetrospectiveStatus.OPEN);
    }

    @Test
    @DisplayName("test-matrix L84: closing a missing retrospective is not found")
    void closeRetrospective_whenRetrospectiveDoesNotExist_throwsNotFoundInStandardFormat() {
        // Act & Assert
        assertThatThrownBy(() -> retrospectives.closeRetrospective(MISSING_ID))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Retrospective " + MISSING_ID + ": not found");
    }

    @Test
    @DisplayName("test-matrix L100, L105: feedback of each ordinary type is stored on an OPEN retrospective")
    void addFeedbackItem_whenRetrospectiveIsOpen_storesFeedbackWithRequestedType() {
        // Arrange
        var retro = newRetrospective();

        // Act
        var positive = retrospectives.addFeedbackItem(retro.getId(), "Good release", "WENT_WELL", SUBMITTER);
        var improvement = retrospectives.addFeedbackItem(retro.getId(), "Slow builds", "NEEDS_IMPROVEMENT", SUBMITTER);

        // Assert
        assertThat(positive.getType()).isEqualTo(FeedbackType.WENT_WELL);
        assertThat(improvement.getType()).isEqualTo(FeedbackType.NEEDS_IMPROVEMENT);
        assertThat(feedbackRepository.findById(positive.getId()).orElseThrow().getRetrospective().getId())
            .isEqualTo(retro.getId());
    }

    @Test
    @DisplayName("test-matrix L101: feedback for a missing retrospective is not found")
    void addFeedbackItem_whenRetrospectiveDoesNotExist_throwsNotFoundAndStoresNothing() {
        // Arrange
        var countBefore = feedbackRepository.count();

        // Act & Assert
        assertThatThrownBy(() -> retrospectives.addFeedbackItem(MISSING_ID, "Orphan", "WENT_WELL", SUBMITTER))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Retrospective " + MISSING_ID + ": not found");
        assertThat(feedbackRepository.count()).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("test-matrix L104: an action item cannot be added to a CLOSED retrospective")
    void addActionItem_whenRetrospectiveIsClosed_rejectsAndStoresNothing() {
        // Arrange
        var retro = newRetrospective();
        retrospectives.closeRetrospective(retro.getId());
        var countBefore = feedbackRepository.count();

        // Act & Assert
        assertThatThrownBy(() -> retrospectives.addActionItem(retro.getId(), "Late action", "HIGH", SUBMITTER))
            .isInstanceOf(RetroflowException.class);
        assertThat(feedbackRepository.count()).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("test-matrix L124: ordinary feedback in a CLOSED retrospective cannot be modified")
    void modifyFeedback_whenRetrospectiveIsClosed_rejectsEditAndKeepsContent() {
        // Arrange
        var retro = newRetrospective();
        var feedback = retrospectives.addFeedbackItem(retro.getId(), "Original", "WENT_WELL", SUBMITTER);
        retrospectives.closeRetrospective(retro.getId());

        // Act & Assert
        assertThatThrownBy(() -> feedback.setContent("Changed")).isInstanceOf(RetroflowException.class);
        assertThatThrownBy(() -> feedback.setSubmittedBy("Bob")).isInstanceOf(RetroflowException.class);
        assertThat(feedback.getContent()).isEqualTo("Original");
        assertThat(feedback.getSubmittedBy()).isEqualTo(SUBMITTER);
    }

    @Test
    @DisplayName("test-matrix L149: an action item is ACTION_ITEM feedback with priority and a boolean state")
    void addActionItem_whenRequestIsValid_storesActionFeedbackThatIsNotCompleted() {
        // Arrange
        var retro = newRetrospective();

        // Act
        var action = retrospectives.addActionItem(retro.getId(), "Fix the build", "HIGH", SUBMITTER);

        // Assert
        var stored = (ActionItem) feedbackRepository.findById(action.getId()).orElseThrow();
        assertThat(stored.getType()).isEqualTo(FeedbackType.ACTION_ITEM);
        assertThat(stored.getContent()).isEqualTo("Fix the build");
        assertThat(stored.getSubmittedBy()).isEqualTo(SUBMITTER);
        assertThat(stored.getRetrospective().getId()).isEqualTo(retro.getId());
        assertThat(stored.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("test-matrix L150: LOW, MEDIUM and HIGH priorities are preserved")
    void addActionItem_whenPriorityIsAllowed_preservesRequestedPriority() {
        // Arrange
        var retro = newRetrospective();

        // Act
        var low = retrospectives.addActionItem(retro.getId(), "Low", "LOW", SUBMITTER);
        var medium = retrospectives.addActionItem(retro.getId(), "Medium", "MEDIUM", SUBMITTER);
        var high = retrospectives.addActionItem(retro.getId(), "High", "HIGH", SUBMITTER);

        // Assert
        assertThat(low.getPriority()).isEqualTo(Priority.LOW);
        assertThat(medium.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(high.getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    @DisplayName("test-matrix L160: completing a missing action item is not found")
    void completeActionItem_whenActionDoesNotExist_throwsNotFoundInStandardFormat() {
        // Act & Assert
        assertThatThrownBy(() -> retrospectives.completeActionItem(MISSING_ID))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("ActionItem " + MISSING_ID + ": not found");
    }

    @Test
    @DisplayName("test-matrix L164: a completed action cannot be uncompleted after its retrospective closes")
    void uncompleteActionItem_whenCompletedActionIsInClosedRetrospective_rejectsAndStaysCompleted() {
        // Arrange
        var retro = newRetrospective();
        var action = retrospectives.addActionItem(retro.getId(), "Fix the build", "HIGH", SUBMITTER);
        retrospectives.completeActionItem(action.getId());
        retrospectives.closeRetrospective(retro.getId());

        // Act & Assert
        assertThatThrownBy(() -> retrospectives.uncompleteActionItem(action.getId()))
            .isInstanceOf(RetroflowException.class);
        assertThat(((ActionItem) feedbackRepository.findById(action.getId()).orElseThrow()).isCompleted()).isTrue();
    }

    @Test
    @DisplayName("test-matrix L167: uncompleting a missing action item is not found")
    void uncompleteActionItem_whenActionDoesNotExist_throwsNotFoundInStandardFormat() {
        // Act & Assert
        assertThatThrownBy(() -> retrospectives.uncompleteActionItem(MISSING_ID))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("ActionItem " + MISSING_ID + ": not found");
    }

    private Team newTeam() {
        return teams.createTeam("Team " + System.nanoTime(), List.of(SUBMITTER, "Bob"));
    }

    private Retrospective newRetrospective() {
        return retrospectives.createRetrospective(newTeam().getId(), "Retro", MEETING_DATE);
    }
}
