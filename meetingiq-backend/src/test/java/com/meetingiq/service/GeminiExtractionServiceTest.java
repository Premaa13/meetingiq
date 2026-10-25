package com.meetingiq.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetingiq.config.MeetingIqProperties;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiExtractionServiceTest {

    @Test
    void extractsTranscriptWithExistingModelAndNormalizesResult() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiExtractionService service = new GeminiExtractionService(
                properties(),
                builder,
                new ObjectMapper());
        String generatedJson = """
                {"title":null,"meeting_date":null,"key_points":["Decision noted"],"action_items":[
                  {"task":"Send the notes","owner":null,"deadline":"Friday","deadline_date":null,"confidence":"high"}
                ]}
                """;
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent"))
                .andExpect(header("x-goog-api-key", "test-gemini-key"))
                .andRespond(withSuccess(geminiResponse(generatedJson), MediaType.APPLICATION_JSON));

        var result = service.extractTranscript("A meeting transcript");

        assertEquals("Meeting on " + LocalDate.now(), result.title());
        assertEquals(LocalDate.now().toString(), result.meetingDate());
        assertEquals("low", result.actionItems().get(0).confidence());
        assertEquals("Decision noted", result.keyPoints().get(0));
        server.verify();
    }

    @Test
    void sendsAudioAsInlineMediaUsingExistingAudioModel() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiExtractionService service = new GeminiExtractionService(
                properties(),
                builder,
                new ObjectMapper());
        byte[] audio = "sample audio".getBytes(StandardCharsets.UTF_8);
        String audioBase64 = Base64.getEncoder().encodeToString(audio);

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent"))
                .andExpect(header("x-goog-api-key", "test-gemini-key"))
                .andExpect(content().string(containsString("\"mime_type\":\"audio/webm\"")))
                .andExpect(content().string(containsString(audioBase64)))
                .andRespond(withSuccess(geminiResponse(
                        "{\"title\":\"Team sync\",\"meeting_date\":\"2026-10-24\",\"key_points\":[],\"action_items\":[]}"),
                        MediaType.APPLICATION_JSON));

        var result = service.extractAudio(audio, "audio/webm");

        assertEquals("Team sync", result.title());
        assertEquals(List.of(), result.keyPoints());
        server.verify();
    }

    private String geminiResponse(String generatedJson) {
        try {
            String escaped = new ObjectMapper().writeValueAsString(generatedJson);
            return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":" + escaped + "}]}}]}";
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private MeetingIqProperties properties() {
        return new MeetingIqProperties(
                "https://supabase.example",
                "test-service-key",
                "test-gemini-key");
    }
}
