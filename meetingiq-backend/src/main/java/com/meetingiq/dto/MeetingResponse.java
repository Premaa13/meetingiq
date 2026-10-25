package com.meetingiq.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MeetingResponse(
        @JsonProperty("meeting_id") String meetingId,
        String title,
        @JsonProperty("meeting_date") String meetingDate,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("action_items") List<ActionItemResponse> actionItems,
        @JsonProperty("key_points") List<String> keyPoints) {
}
