package com.stackcraft.retroflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateActionItemRequest(
    @NotBlank @Size(max = 10_000) String content,
    @NotBlank @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "must be LOW, MEDIUM or HIGH") String priority,
    @NotBlank @Size(max = 200) String submittedBy
) {
}
