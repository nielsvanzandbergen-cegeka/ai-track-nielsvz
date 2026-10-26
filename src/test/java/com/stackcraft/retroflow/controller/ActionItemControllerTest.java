package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ActionItemControllerTest extends ControllerTestSupport {

    private static final Long ACTION_ID = 200L;
    private static final String COMPLETE_URL = "/api/action-items/{id}/complete";
    private static final String UNCOMPLETE_URL = "/api/action-items/{id}/uncomplete";

    @Autowired
    ActionItemControllerTest(MockMvc mvc, TeamService teams, RetrospectiveService retrospectives) {
        super(mvc, teams, retrospectives);
    }

    @Test
    void completeActionItem_whenActionExists_returnsNoContent() throws Exception {
        // Act & Assert
        mvc.perform(put(COMPLETE_URL, ACTION_ID))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));
        verify(retrospectives).completeActionItem(ACTION_ID);
    }

    @Test
    void completeActionItem_whenActionIsMissing_returnsNotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("ActionItem", ACTION_ID))
            .when(retrospectives).completeActionItem(ACTION_ID);

        // Act & Assert
        mvc.perform(put(COMPLETE_URL, ACTION_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ActionItem " + ACTION_ID + ": not found"))
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void uncompleteActionItem_whenActionIsCompleted_returnsConflict() throws Exception {
        // Arrange
        var message = "ActionItem " + ACTION_ID + ": already completed, cannot be undone";
        doThrow(new RetroflowException(message)).when(retrospectives).uncompleteActionItem(ACTION_ID);

        // Act & Assert
        mvc.perform(put(UNCOMPLETE_URL, ACTION_ID))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value(message))
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void uncompleteActionItem_whenActionIsNotCompleted_returnsNoContent() throws Exception {
        // Act & Assert
        mvc.perform(put(UNCOMPLETE_URL, ACTION_ID))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));
        verify(retrospectives).uncompleteActionItem(ACTION_ID);
    }

    @Test
    void uncompleteActionItem_whenActionIsMissing_returnsNotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("ActionItem", ACTION_ID))
            .when(retrospectives).uncompleteActionItem(ACTION_ID);

        // Act & Assert
        mvc.perform(put(UNCOMPLETE_URL, ACTION_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("ActionItem " + ACTION_ID + ": not found"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-number", "999999999999999999999999"})
    void actionItemEndpoints_whenIdIsMalformed_returnBadRequestBeforeService(String id) throws Exception {
        // Act & Assert
        mvc.perform(put(COMPLETE_URL, id))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(containsString("id")));
        mvc.perform(put(UNCOMPLETE_URL, id))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(teams, retrospectives);
    }
}
