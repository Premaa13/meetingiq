package com.meetingiq.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.meetingiq.config.MeetingIqProperties;
import com.meetingiq.exception.ExternalServiceException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class SupabaseDataService {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> ROWS =
            new ParameterizedTypeReference<>() { };

    private final MeetingIqProperties properties;
    private final RestClient restClient;

    public SupabaseDataService(MeetingIqProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.clone()
                .baseUrl(properties.supabaseUrl() + "/rest/v1")
                .build();
    }

    public List<Map<String, Object>> findMeetings(String userId) {
        return getRows("meetings", Map.of(
                "select", "id,user_id,title,meeting_date,created_at",
                "user_id", "eq." + userId,
                "order", "created_at.desc"));
    }

    public List<Map<String, Object>> findMeeting(String userId, String meetingId) {
        return getRows("meetings", Map.of(
                "select", "id,user_id,title,meeting_date,created_at",
                "id", "eq." + meetingId,
                "user_id", "eq." + userId));
    }

    public List<Map<String, Object>> findActionItems(String userId, List<String> meetingIds) {
        if (meetingIds.isEmpty()) {
            return List.of();
        }
        return getRows("action_items", Map.of(
                "select", "meeting_id,task,owner,deadline,deadline_date,confidence,is_completed",
                "user_id", "eq." + userId,
                "meeting_id", "in.(" + String.join(",", meetingIds) + ")"));
    }

    public List<Map<String, Object>> findActionItems(String userId, String meetingId) {
        return getRows("action_items", Map.of(
                "select", "meeting_id,task,owner,deadline,deadline_date,confidence,is_completed",
                "meeting_id", "eq." + meetingId,
                "user_id", "eq." + userId));
    }

    public List<Map<String, Object>> findKeyPoints(String userId, List<String> meetingIds) {
        if (meetingIds.isEmpty()) {
            return List.of();
        }
        return getRows("key_points", Map.of(
                "select", "meeting_id,point",
                "user_id", "eq." + userId,
                "meeting_id", "in.(" + String.join(",", meetingIds) + ")"));
    }

    public List<Map<String, Object>> findKeyPoints(String userId, String meetingId) {
        return getRows("key_points", Map.of(
                "select", "meeting_id,point",
                "meeting_id", "eq." + meetingId,
                "user_id", "eq." + userId));
    }

    public String insertMeeting(String userId, String title, String meetingDate, String transcript) {
        Map<String, Object> meeting = new LinkedHashMap<>();
        meeting.put("user_id", userId);
        meeting.put("title", title);
        meeting.put("meeting_date", meetingDate);
        meeting.put("transcript", transcript);

        try {
            List<Map<String, Object>> rows = restClient.post()
                    .uri("/meetings")
                    .headers(this::addServiceHeaders)
                    .header("Prefer", "return=representation")
                    .body(List.of(meeting))
                    .retrieve()
                    .body(ROWS);

            if (rows == null || rows.isEmpty() || rows.get(0).get("id") == null) {
                throw new ExternalServiceException("Supabase did not return the new meeting ID");
            }
            return rows.get(0).get("id").toString();
        } catch (RestClientException exception) {
            throw new ExternalServiceException("Supabase meeting insert failed", exception);
        }
    }

    public void insertActionItems(String userId, String meetingId, List<com.meetingiq.dto.ExtractedActionItem> items) {
        if (items.isEmpty()) {
            return;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (var item : items) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("meeting_id", meetingId);
            row.put("user_id", userId);
            row.put("task", item.task());
            row.put("owner", item.owner());
            row.put("deadline", item.deadline());
            row.put("deadline_date", item.deadlineDate());
            row.put("confidence", item.confidence());
            row.put("is_completed", false);
            rows.add(row);
        }
        insertRows("action_items", rows);
    }

    public void insertKeyPoints(String userId, String meetingId, List<String> points) {
        if (points.isEmpty()) {
            return;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String point : points) {
            rows.add(Map.of(
                    "meeting_id", meetingId,
                    "user_id", userId,
                    "point", point));
        }
        insertRows("key_points", rows);
    }

    private List<Map<String, Object>> getRows(String table, Map<String, String> query) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/" + table);
                        query.forEach(builder::queryParam);
                        return builder.build();
                    })
                    .headers(this::addServiceHeaders)
                    .retrieve()
                    .body(ROWS);
        } catch (RestClientException exception) {
            throw new ExternalServiceException("Supabase query failed", exception);
        }
    }

    private void insertRows(String table, List<Map<String, Object>> rows) {
        try {
            restClient.post()
                    .uri("/" + table)
                    .headers(this::addServiceHeaders)
                    .body(rows)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new ExternalServiceException("Supabase insert failed", exception);
        }
    }

    private void addServiceHeaders(org.springframework.http.HttpHeaders headers) {
        headers.setBearerAuth(properties.supabaseServiceKey());
        headers.set("apikey", properties.supabaseServiceKey());
    }
}
