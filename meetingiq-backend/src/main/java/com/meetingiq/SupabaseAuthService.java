package com.meetingiq;

import com.meetingiq.config.MeetingIqProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Service
public class SupabaseAuthService {

    private final MeetingIqProperties properties;
    private final RestClient restClient;

    public SupabaseAuthService(MeetingIqProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        requireConfigured(properties.supabaseUrl(), "SUPABASE_URL");
        requireConfigured(properties.supabaseServiceKey(), "SUPABASE_SERVICE_KEY");
        this.restClient = restClientBuilder.clone()
                .baseUrl(properties.supabaseUrl() + "/auth/v1")
                .build();
    }

    public String getAuthenticatedUserId(String authorization) {

        if (authorization == null || authorization.isBlank()
                || !authorization.startsWith("Bearer ")
                || authorization.substring(7).isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Missing auth token"
            );
        }

        String token = authorization.substring(7).trim();

        try {
            Map<?, ?> user = restClient.get()
                    .uri("/user")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("apikey", properties.supabaseServiceKey())
                    .retrieve()
                    .body(Map.class);

            if (user == null || user.get("id") == null) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid or expired token"
                );
            }

            return user.get("id").toString();

        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or expired token"
            );
        }
    }

    private static void requireConfigured(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is missing");
        }
    }
}
