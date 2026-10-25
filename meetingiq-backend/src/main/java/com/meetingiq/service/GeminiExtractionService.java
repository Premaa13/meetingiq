package com.meetingiq.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetingiq.config.MeetingIqProperties;
import com.meetingiq.dto.ExtractedActionItem;
import com.meetingiq.dto.ExtractionResult;
import com.meetingiq.exception.ExternalServiceException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class GeminiExtractionService {

    private static final Pattern JSON_FENCE = Pattern.compile("^```(?:json)?\\s*|\\s*```$", Pattern.MULTILINE);

    private final MeetingIqProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiExtractionService(
            MeetingIqProperties properties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        if (properties.geminiApiKey() == null || properties.geminiApiKey().isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is missing");
        }
        this.restClient = restClientBuilder.clone()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    public ExtractionResult extractTranscript(String transcript) {
        String prompt = buildPrompt(
                "Extract meeting info and action items from this transcript.",
                "\n\nTranscript:\n" + transcript);
        return extract("gemini-flash-lite-latest", List.of(Map.of("text", prompt)));
    }

    public ExtractionResult extractAudio(byte[] audioBytes, String mimeType) {
        String prompt = buildPrompt("Listen to this meeting recording. Extract meeting info and action items.", "");
        Map<String, Object> audioPart = Map.of(
                "inline_data", Map.of(
                        "mime_type", mimeType,
                        "data", Base64.getEncoder().encodeToString(audioBytes)));
        return extract("gemini-flash-latest", List.of(Map.of("text", prompt), audioPart));
    }

    private ExtractionResult extract(String model, List<Map<String, Object>> parts) {
        try {
            JsonNode response = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", properties.geminiApiKey())
                    .body(Map.of("contents", List.of(Map.of("parts", parts))))
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode textNode = response == null
                    ? null
                    : response.at("/candidates/0/content/parts/0/text");
            if (textNode == null || textNode.isMissingNode() || !textNode.isTextual()) {
                throw new ExternalServiceException("Gemini returned no extraction content");
            }

            String json = JSON_FENCE.matcher(textNode.textValue().trim()).replaceAll("").trim();
            ExtractionResult result = objectMapper.readValue(json, ExtractionResult.class);
            return normalize(result);
        } catch (RestClientException exception) {
            throw new ExternalServiceException("Gemini extraction request failed", exception);
        } catch (JsonProcessingException exception) {
            throw new ExternalServiceException("Gemini returned invalid extraction JSON", exception);
        }
    }

    private ExtractionResult normalize(ExtractionResult result) {
        if (result == null || result.actionItems() == null) {
            throw new ExternalServiceException("Gemini returned an incomplete extraction");
        }

        String meetingDate = result.meetingDate();
        if (meetingDate == null || meetingDate.isBlank()) {
            meetingDate = LocalDate.now().toString();
        }
        String title = result.title();
        if (title == null || title.isBlank()) {
            title = "Meeting on " + meetingDate;
        }

        List<ExtractedActionItem> normalizedItems = new ArrayList<>();
        for (ExtractedActionItem item : result.actionItems()) {
            if (item == null || item.task() == null || item.task().isBlank()) {
                throw new ExternalServiceException("Gemini returned an action item without a task");
            }
            String confidence = item.confidence();
            if (item.owner() == null || item.deadline() == null) {
                confidence = "low";
            }
            normalizedItems.add(new ExtractedActionItem(
                    item.task(),
                    item.owner(),
                    item.deadline(),
                    item.deadlineDate(),
                    confidence));
        }

        return new ExtractionResult(
                title,
                meetingDate,
                result.keyPoints() == null ? List.of() : result.keyPoints(),
                normalizedItems);
    }

    private String buildPrompt(String instruction, String transcriptSuffix) {
        return """
                Today's date is %s. If a deadline is relative (e.g. "next Friday", "in two weeks"), calculate deadline_date as an actual YYYY-MM-DD date based on today. If no deadline is mentioned, both deadline and deadline_date should be null.

                %s
                Return ONLY a JSON object, no other text, in this exact format:
                {
                  "title": "short descriptive title, or null if unclear",
                  "meeting_date": "YYYY-MM-DD if mentioned, or null if not mentioned",
                  "key_points": ["any notable fact, announcement, or context mentioned — e.g. schedule changes, personal notes like a birthday, overtime/workload mentions, heads-ups, or anything worth remembering later, even if casual or unrelated to action items"],
                  "action_items": [
                    {"task": "...", "owner": "... or null", "deadline": "... or null", "deadline_date": "YYYY-MM-DD or null", "confidence": "high or low"}
                  ]
                }%s
                """.formatted(LocalDate.now(), instruction, transcriptSuffix);
    }
}
