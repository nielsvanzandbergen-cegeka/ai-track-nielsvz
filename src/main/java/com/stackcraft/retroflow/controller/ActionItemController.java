package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.service.RetrospectiveService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/action-items/{id}")
public class ActionItemController {

    private final RetrospectiveService retrospectiveService;

    public ActionItemController(RetrospectiveService retrospectiveService) {
        this.retrospectiveService = retrospectiveService;
    }

    @PutMapping("/complete")
    public ResponseEntity<Void> completeActionItem(@PathVariable @NotNull Long id) {
        retrospectiveService.completeActionItem(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/uncomplete")
    public ResponseEntity<Void> uncompleteActionItem(@PathVariable @NotNull Long id) {
        retrospectiveService.uncompleteActionItem(id);
        return ResponseEntity.noContent().build();
    }
}
