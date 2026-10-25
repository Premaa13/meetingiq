package com.meetingiq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ActionItemResponse(
        String task,
        String owner,
        String deadline,
        @JsonProperty("deadline_date") String deadlineDate,
        String confidence,
        @JsonProperty("is_completed") boolean isCompleted) {
}
