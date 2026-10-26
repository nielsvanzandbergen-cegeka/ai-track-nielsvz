package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.ActionItemResponse;
import com.stackcraft.retroflow.dto.CreateActionItemRequest;
import com.stackcraft.retroflow.dto.CreateFeedbackRequest;
import com.stackcraft.retroflow.dto.FeedbackResponse;
import com.stackcraft.retroflow.service.RetrospectiveService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/retrospectives/{retrospectiveId}")
public class FeedbackController {

    private final RetrospectiveService retrospectiveService;

    public FeedbackController(RetrospectiveService retrospectiveService) {
        this.retrospectiveService = retrospectiveService;
    }

    @PostMapping("/feedback")
    public ResponseEntity<FeedbackResponse> addFeedbackItem(
            @PathVariable @NotNull Long retrospectiveId,
            @Valid @RequestBody CreateFeedbackRequest request) {
        var feedback = retrospectiveService.addFeedbackItem(
            retrospectiveId, request.content(), request.type(), request.submittedBy());
        return ResponseEntity.created(location("/api/feedback/{id}", feedback.getId()))
            .body(FeedbackResponse.from(feedback));
    }

    @PostMapping("/action-items")
    public ResponseEntity<ActionItemResponse> addActionItem(
            @PathVariable @NotNull Long retrospectiveId,
            @Valid @RequestBody CreateActionItemRequest request) {
        var action = retrospectiveService.addActionItem(
            retrospectiveId, request.content(), request.priority(), request.submittedBy());
        return ResponseEntity.created(location("/api/action-items/{id}", action.getId()))
            .body(ActionItemResponse.from(action));
    }

    private URI location(String path, Long id) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(path).buildAndExpand(id).toUri();
    }
}
