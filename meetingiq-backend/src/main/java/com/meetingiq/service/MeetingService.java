package com.meetingiq.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.meetingiq.SupabaseAuthService;
import com.meetingiq.dto.ActionItemResponse;
import com.meetingiq.dto.ExtractedActionItem;
import com.meetingiq.dto.ExtractionResponse;
import com.meetingiq.dto.ExtractionResult;
import com.meetingiq.dto.MeetingResponse;
import com.meetingiq.exception.InvalidRequestException;
import com.meetingiq.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MeetingService {

    private final SupabaseAuthService authService;
    private final SupabaseDataService dataService;
    private final GeminiExtractionService extractionService;

    public MeetingService(
            SupabaseAuthService authService,
            SupabaseDataService dataService,
            GeminiExtractionService extractionService) {
        this.authService = authService;
        this.dataService = dataService;
        this.extractionService = extractionService;
    }

    public List<MeetingResponse> getMeetings(String authorization) {
        String userId = authService.getAuthenticatedUserId(authorization);
        List<Map<String, Object>> meetings = dataService.findMeetings(userId);
        List<String> meetingIds = meetings.stream()
                .map(meeting -> value(meeting, "id"))
                .toList();

        Map<String, List<Map<String, Object>>> actionsByMeeting =
                groupByMeetingId(dataService.findActionItems(userId, meetingIds));
        Map<String, List<Map<String, Object>>> pointsByMeeting =
                groupByMeetingId(dataService.findKeyPoints(userId, meetingIds));

        return meetings.stream()
                .map(meeting -> buildMeetingResponse(
                        meeting,
                        actionsByMeeting.getOrDefault(value(meeting, "id"), List.of()),
                        pointsByMeeting.getOrDefault(value(meeting, "id"), List.of())))
                .toList();
    }

    public MeetingResponse getMeeting(String authorization, String meetingId) {
        String userId = authService.getAuthenticatedUserId(authorization);
        List<Map<String, Object>> meetings = dataService.findMeeting(userId, meetingId);
        if (meetings.isEmpty()) {
            throw new ResourceNotFoundException("Meeting not found");
        }

        Map<String, Object> meeting = meetings.get(0);
        return buildMeetingResponse(
                meeting,
                dataService.findActionItems(userId, meetingId),
                dataService.findKeyPoints(userId, meetingId));
    }

    public ExtractionResponse extractTranscript(String authorization, String transcript) {
        String userId = authService.getAuthenticatedUserId(authorization);
        ExtractionResult result = extractionService.extractTranscript(transcript);
        String meetingId = persistResult(userId, transcript, result);
        return toExtractionResponse(meetingId, result);
    }

    public ExtractionResponse extractAudio(
            String authorization,
            byte[] audioBytes,
            String mimeType,
            String filename) {
        String userId = authService.getAuthenticatedUserId(authorization);
        if (audioBytes.length == 0) {
            throw new InvalidRequestException("Audio file must not be empty");
        }
        if (mimeType == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only audio and video files are supported");
        }
        ExtractionResult result = extractionService.extractAudio(audioBytes, mimeType);
        String transcript = "[Audio recording: " + filename + "]";
        String meetingId = persistResult(userId, transcript, result);
        return toExtractionResponse(meetingId, result);
    }

    private String persistResult(String userId, String transcript, ExtractionResult result) {
        String meetingId = dataService.insertMeeting(
                userId,
                result.title(),
                result.meetingDate(),
                transcript);
        dataService.insertKeyPoints(userId, meetingId, result.keyPoints());
        dataService.insertActionItems(userId, meetingId, result.actionItems());
        return meetingId;
    }

    private MeetingResponse buildMeetingResponse(
            Map<String, Object> meeting,
            List<Map<String, Object>> actionItems,
            List<Map<String, Object>> keyPoints) {
        List<ActionItemResponse> itemResponses = actionItems.stream()
                .map(item -> new ActionItemResponse(
                        value(item, "task"),
                        nullableValue(item, "owner"),
                        nullableValue(item, "deadline"),
                        nullableValue(item, "deadline_date"),
                        nullableValue(item, "confidence"),
                        Boolean.TRUE.equals(item.get("is_completed"))))
                .toList();
        List<String> pointResponses = keyPoints.stream()
                .map(point -> value(point, "point"))
                .toList();
        return new MeetingResponse(
                value(meeting, "id"),
                nullableValue(meeting, "title"),
                nullableValue(meeting, "meeting_date"),
                nullableValue(meeting, "created_at"),
                itemResponses,
                pointResponses);
    }

    private Map<String, List<Map<String, Object>>> groupByMeetingId(List<Map<String, Object>> rows) {
        Map<String, List<Map<String, Object>>> grouped = new HashMap<>();
        for (Map<String, Object> row : rows) {
            grouped.computeIfAbsent(value(row, "meeting_id"), ignored -> new ArrayList<>()).add(row);
        }
        return grouped;
    }

    private ExtractionResponse toExtractionResponse(String meetingId, ExtractionResult result) {
        return new ExtractionResponse(
                meetingId,
                result.title(),
                result.meetingDate(),
                result.keyPoints(),
                result.actionItems());
    }

    private static String value(Map<String, Object> row, String field) {
        String value = nullableValue(row, field);
        if (value == null) {
            throw new IllegalStateException("Supabase response is missing " + field);
        }
        return value;
    }

    private static String nullableValue(Map<String, Object> row, String field) {
        Object value = row.get(field);
        return value == null ? null : value.toString();
    }
}
