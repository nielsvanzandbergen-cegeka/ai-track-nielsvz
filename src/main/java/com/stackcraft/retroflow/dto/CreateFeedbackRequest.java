package com.stackcraft.retroflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateFeedbackRequest(
    @NotBlank @Size(max = 10_000) String content,
    @NotBlank @Pattern(regexp = "WENT_WELL|NEEDS_IMPROVEMENT", message = "must be WENT_WELL or NEEDS_IMPROVEMENT")
    String type,
    @NotBlank @Size(max = 200) String submittedBy
) {
}
