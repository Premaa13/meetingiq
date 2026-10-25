package com.meetingiq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.meetingiq.config.MeetingIqProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class SupabaseAuthServiceTest {

    @Test
    void rejectsMissingAuthorizationHeader() {
        SupabaseAuthService service = newService();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getAuthenticatedUserId(null));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Missing auth token", exception.getReason());
    }

    @Test
    void rejectsInvalidSupabaseAccessToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SupabaseAuthService service = new SupabaseAuthService(properties(), builder);
        server.expect(requestTo("https://supabase.example/auth/v1/user"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer invalid-token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getAuthenticatedUserId("Bearer invalid-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Invalid or expired token", exception.getReason());
        server.verify();
    }

    @Test
    void returnsAuthenticatedSupabaseUserId() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SupabaseAuthService service = new SupabaseAuthService(properties(), builder);
        server.expect(requestTo("https://supabase.example/auth/v1/user"))
                .andExpect(header("Authorization", "Bearer valid-token"))
                .andRespond(withSuccess("{\"id\":\"user-123\"}", MediaType.APPLICATION_JSON));

        assertEquals("user-123", service.getAuthenticatedUserId("Bearer valid-token"));
        server.verify();
    }

    private SupabaseAuthService newService() {
        return new SupabaseAuthService(properties(), RestClient.builder());
    }

    private MeetingIqProperties properties() {
        return new MeetingIqProperties(
                "https://supabase.example",
                "test-service-key",
                "test-gemini-key");
    }
}
