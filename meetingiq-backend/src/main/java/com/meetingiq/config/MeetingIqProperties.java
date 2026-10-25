package com.meetingiq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "meetingiq")
public record MeetingIqProperties(
        String supabaseUrl,
        String supabaseServiceKey,
        String geminiApiKey) {
}
