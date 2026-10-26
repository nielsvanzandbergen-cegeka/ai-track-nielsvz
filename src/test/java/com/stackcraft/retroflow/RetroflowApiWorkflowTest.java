package com.stackcraft.retroflow;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RetroflowApiWorkflowTest {

    private static final String TEAM_JSON = """
        {"name":"Workflow team","members":["Alice","Bob"]}
        """;
    private static final String RETRO_JSON = """
        {"title":"Sprint 12","date":"2026-10-26"}
        """;
    private static final String FEEDBACK_JSON = """
        {"content":"Fast reviews","type":"WENT_WELL","submittedBy":"Alice"}
        """;
    private static final String ACTION_JSON = """
        {"content":"Automate deployment","priority":"HIGH","submittedBy":"Bob"}
        """;

    private final MockMvc mvc;

    @Autowired
    RetroflowApiWorkflowTest(MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test
    void retrospectiveWorkflow_whenFollowedEndToEnd_enforcesLifecycleRules() throws Exception {
        // Arrange
        var teamId = idOf(postJson("/api/teams", TEAM_JSON).andExpect(status().isCreated()));
        var retroId = idOf(postJson("/api/teams/" + teamId + "/retrospectives", RETRO_JSON)
            .andExpect(status().isCreated()));

        // Act
        postJson("/api/retrospectives/" + retroId + "/feedback", FEEDBACK_JSON).andExpect(status().isCreated());
        var actionId = idOf(postJson("/api/retrospectives/" + retroId + "/action-items", ACTION_JSON)
            .andExpect(status().isCreated()).andExpect(jsonPath("$.completed").value(false)));
        mvc.perform(put("/api/retrospectives/{id}/close", retroId)).andExpect(status().isNoContent());

        // Assert
        postJson("/api/teams/" + teamId + "/retrospectives", RETRO_JSON).andExpect(status().isCreated());
        postJson("/api/retrospectives/" + retroId + "/feedback", FEEDBACK_JSON)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("Retrospective " + retroId + ": cannot add feedback to a closed retrospective"));
        mvc.perform(put("/api/action-items/{id}/complete", actionId)).andExpect(status().isNoContent());
        mvc.perform(put("/api/action-items/{id}/uncomplete", actionId)).andExpect(status().isConflict());
    }

    @Test
    void createRetrospective_whenTeamAlreadyHasOpenRetrospective_returnsConflictOverHttp() throws Exception {
        // Arrange
        var teamId = idOf(postJson("/api/teams", TEAM_JSON));
        postJson("/api/teams/" + teamId + "/retrospectives", RETRO_JSON).andExpect(status().isCreated());

        // Act & Assert
        postJson("/api/teams/" + teamId + "/retrospectives", RETRO_JSON)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long idOf(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }
}
