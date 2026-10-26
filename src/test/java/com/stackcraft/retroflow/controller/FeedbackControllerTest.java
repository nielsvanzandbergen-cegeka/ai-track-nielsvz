package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.entity.WentWellFeedback;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FeedbackControllerTest extends ControllerTestSupport {

    private static final Long FEEDBACK_ID = 100L;
    private static final Long ACTION_ID = 200L;
    private static final String CONTENT = "Fast reviews";
    private static final String SUBMITTER = "Alice";
    private static final String FEEDBACK_URL = "/api/retrospectives/{retrospectiveId}/feedback";
    private static final String ACTION_URL = "/api/retrospectives/{retrospectiveId}/action-items";
    private static final String FEEDBACK_JSON = """
        {"content":"Fast reviews","type":"WENT_WELL","submittedBy":"Alice"}
        """;
    private static final String ACTION_JSON = """
        {"content":"Fast reviews","priority":"HIGH","submittedBy":"Alice"}
        """;

    @Autowired
    FeedbackControllerTest(MockMvc mvc, TeamService teams, RetrospectiveService retrospectives) {
        super(mvc, teams, retrospectives);
    }

    @Test
    void addFeedbackItem_whenRequestIsValid_returnsCreatedFeedback() throws Exception {
        // Arrange
        var feedback = new WentWellFeedback(FEEDBACK_ID, 0, retrospective(), CONTENT, SUBMITTER);
        when(retrospectives.addFeedbackItem(RETRO_ID, CONTENT, "WENT_WELL", SUBMITTER)).thenReturn(feedback);

        // Act & Assert
        mvc.perform(json(FEEDBACK_URL, FEEDBACK_JSON))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/api/feedback/" + FEEDBACK_ID))
            .andExpect(jsonPath("$.id").value(FEEDBACK_ID.intValue()))
            .andExpect(jsonPath("$.retrospectiveId").value(RETRO_ID.intValue()))
            .andExpect(jsonPath("$.content").value(CONTENT))
            .andExpect(jsonPath("$.submittedBy").value(SUBMITTER))
            .andExpect(jsonPath("$.type").value("WENT_WELL"))
            .andExpect(jsonPath("$.retrospective").doesNotExist());
    }

    @Test
    void addFeedbackItem_whenRetrospectiveIsMissing_returnsNotFound() throws Exception {
        // Arrange
        when(retrospectives.addFeedbackItem(RETRO_ID, CONTENT, "WENT_WELL", SUBMITTER))
            .thenThrow(new ResourceNotFoundException("Retrospective", RETRO_ID));

        // Act & Assert
        mvc.perform(json(FEEDBACK_URL, FEEDBACK_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Retrospective " + RETRO_ID + ": not found"))
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void addFeedbackItem_whenRetrospectiveIsClosed_returnsConflict() throws Exception {
        // Arrange
        var message = "Retrospective " + RETRO_ID + ": cannot add feedback to a closed retrospective";
        when(retrospectives.addFeedbackItem(RETRO_ID, CONTENT, "WENT_WELL", SUBMITTER))
            .thenThrow(new RetroflowException(message));

        // Act & Assert
        mvc.perform(json(FEEDBACK_URL, FEEDBACK_JSON))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value(message))
            .andExpect(jsonPath("$.status").value(409));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"content\":\"Fast reviews\",\"type\":\"UNKNOWN\",\"submittedBy\":\"Alice\"}",
        "{\"content\":\"Fast reviews\",\"type\":\"went_well\",\"submittedBy\":\"Alice\"}",
        "{\"content\":\"Fast reviews\",\"submittedBy\":\"Alice\"}"
    })
    void addFeedbackItem_whenTypeIsInvalid_returnsBadRequestBeforeService(String body) throws Exception {
        // Act & Assert
        mvc.perform(json(FEEDBACK_URL, body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(containsString("type")));
        verifyNoInteractions(teams, retrospectives);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"content\":\" \",\"type\":\"WENT_WELL\",\"submittedBy\":\"Alice\"}",
        "{\"content\":\"Fast reviews\",\"type\":\"WENT_WELL\",\"submittedBy\":\" \"}",
        "{\"content\":\"Fast reviews\",\"type\":\"WENT_WELL\"}",
        "{\"content\":\"Fast reviews\",\"type\":\"WENT_WELL\",\"submittedBy\":\"Alice\",\"priority\":\"HIGH\"}",
        "{\"content\":123,\"type\":\"WENT_WELL\",\"submittedBy\":\"Alice\"}",
        "not-json"
    })
    void addFeedbackItem_whenBodyIsInvalid_returnsBadRequestBeforeService(String body) throws Exception {
        // Act & Assert
        mvc.perform(json(FEEDBACK_URL, body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.timestamp").exists());
        verifyNoInteractions(teams, retrospectives);
    }

    @Test
    void addFeedbackItem_whenSubmitterIsTooLong_returnsBadRequestBeforeService() throws Exception {
        // Arrange
        var body = "{\"content\":\"Fast reviews\",\"type\":\"WENT_WELL\",\"submittedBy\":\"" + "a".repeat(201) + "\"}";

        // Act & Assert
        mvc.perform(json(FEEDBACK_URL, body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(containsString("submittedBy")));
        verifyNoInteractions(teams, retrospectives);
    }

    @Test
    void addFeedbackItem_whenRetrospectiveIdIsMalformed_returnsBadRequestBeforeService() throws Exception {
        // Act & Assert
        mvc.perform(post(FEEDBACK_URL, "not-a-number").contentType(MediaType.APPLICATION_JSON).content(FEEDBACK_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(containsString("retrospectiveId")));
        verifyNoInteractions(teams, retrospectives);
    }

    @Test
    void addActionItem_whenRequestIsValid_returnsCreatedIncompleteAction() throws Exception {
        // Arrange
        var action = new ActionItem(ACTION_ID, 0, retrospective(), CONTENT, SUBMITTER, Priority.HIGH, false);
        when(retrospectives.addActionItem(RETRO_ID, CONTENT, "HIGH", SUBMITTER)).thenReturn(action);

        // Act & Assert
        mvc.perform(json(ACTION_URL, ACTION_JSON))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/api/action-items/" + ACTION_ID))
            .andExpect(jsonPath("$.id").value(ACTION_ID.intValue()))
            .andExpect(jsonPath("$.retrospectiveId").value(RETRO_ID.intValue()))
            .andExpect(jsonPath("$.type").value("ACTION_ITEM"))
            .andExpect(jsonPath("$.priority").value("HIGH"))
            .andExpect(jsonPath("$.completed").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOW", "MEDIUM", "HIGH"})
    void addActionItem_whenPriorityIsAllowed_passesPriorityToService(String priority) throws Exception {
        // Arrange
        var action = new ActionItem(ACTION_ID, 0, retrospective(), CONTENT, SUBMITTER,
            Priority.valueOf(priority), false);
        when(retrospectives.addActionItem(RETRO_ID, CONTENT, priority, SUBMITTER)).thenReturn(action);
        var body = "{\"content\":\"Fast reviews\",\"priority\":\"" + priority + "\",\"submittedBy\":\"Alice\"}";

        // Act & Assert
        mvc.perform(json(ACTION_URL, body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.priority").value(priority));
        verify(retrospectives).addActionItem(RETRO_ID, CONTENT, priority, SUBMITTER);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"content\":\"Fast reviews\",\"priority\":\"URGENT\",\"submittedBy\":\"Alice\"}",
        "{\"content\":\"Fast reviews\",\"priority\":\"high\",\"submittedBy\":\"Alice\"}",
        "{\"content\":\"Fast reviews\",\"submittedBy\":\"Alice\"}"
    })
    void addActionItem_whenPriorityIsInvalid_returnsBadRequestBeforeService(String body) throws Exception {
        // Act & Assert
        mvc.perform(json(ACTION_URL, body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(containsString("priority")));
        verifyNoInteractions(teams, retrospectives);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"content\":\"Fast reviews\",\"priority\":\"HIGH\",\"submittedBy\":\"Alice\",\"completed\":true}",
        "{\"content\":\"Fast reviews\",\"priority\":\"HIGH\",\"submittedBy\":\"Alice\",\"completed\":\"maybe\"}",
        "{\"content\":\" \",\"priority\":\"HIGH\",\"submittedBy\":\"Alice\"}",
        "{\"content\":\"Fast reviews\",\"priority\":\"HIGH\"}"
    })
    void addActionItem_whenBodyIsInvalid_returnsBadRequestBeforeService(String body) throws Exception {
        // Act & Assert
        mvc.perform(json(ACTION_URL, body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(teams, retrospectives);
    }

    @Test
    void addActionItem_whenRetrospectiveIsClosed_returnsConflict() throws Exception {
        // Arrange
        var message = "Retrospective " + RETRO_ID + ": cannot add feedback to a closed retrospective";
        when(retrospectives.addActionItem(RETRO_ID, CONTENT, "HIGH", SUBMITTER))
            .thenThrow(new RetroflowException(message));

        // Act & Assert
        mvc.perform(json(ACTION_URL, ACTION_JSON))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value(message));
    }

    @Test
    void addActionItem_whenRetrospectiveIsMissing_returnsNotFound() throws Exception {
        // Arrange
        when(retrospectives.addActionItem(RETRO_ID, CONTENT, "HIGH", SUBMITTER))
            .thenThrow(new ResourceNotFoundException("Retrospective", RETRO_ID));

        // Act & Assert
        mvc.perform(json(ACTION_URL, ACTION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Retrospective " + RETRO_ID + ": not found"));
    }

    private MockHttpServletRequestBuilder json(String url, String body) {
        return post(url, RETRO_ID).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
