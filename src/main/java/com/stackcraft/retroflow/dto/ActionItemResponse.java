package com.stackcraft.retroflow.dto;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.Priority;

public record ActionItemResponse(
    Long id,
    Long retrospectiveId,
    String content,
    String submittedBy,
    FeedbackType type,
    Priority priority,
    boolean completed
) {
    public static ActionItemResponse from(ActionItem action) {
        return new ActionItemResponse(action.getId(), action.getRetrospective().getId(), action.getContent(),
            action.getSubmittedBy(), action.getType(), action.getPriority(), action.isCompleted());
    }
}
