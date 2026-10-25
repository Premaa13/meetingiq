package com.meetingiq.dto;

import jakarta.validation.constraints.NotNull;

public record TranscriptRequest(@NotNull String transcript) {
}
