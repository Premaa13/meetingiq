package com.meetingiq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExtractedActionItem(
        String task,
        String owner,
        String deadline,
        @JsonProperty("deadline_date") String deadlineDate,
        String confidence) {
}
