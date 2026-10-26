package com.stackcraft.retroflow.dto;

import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.FeedbackType;

public record FeedbackResponse(
    Long id,
    Long retrospectiveId,
    String content,
    String submittedBy,
    FeedbackType type
) {
    public static FeedbackResponse from(FeedbackItem feedback) {
        return new FeedbackResponse(feedback.getId(), feedback.getRetrospective().getId(),
            feedback.getContent(), feedback.getSubmittedBy(), feedback.getType());
    }
}
