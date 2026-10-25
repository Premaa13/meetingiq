package com.meetingiq.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExtractionResult(
        String title,
        @JsonProperty("meeting_date") String meetingDate,
        @JsonProperty("key_points") List<String> keyPoints,
        @JsonProperty("action_items") List<ExtractedActionItem> actionItems) {
}
