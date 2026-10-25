package com.meetingiq.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.containsString;

import com.meetingiq.config.MeetingIqProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SupabaseDataServiceTest {

    @Test
    void scopesMeetingLookupToBothMeetingAndAuthenticatedUser() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SupabaseDataService service = new SupabaseDataService(properties(), builder);
        server.expect(requestTo(containsString("/rest/v1/meetings?")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("id", equalTo("eq.meeting-1")))
                .andExpect(queryParam("user_id", equalTo("eq.user-a")))
                .andExpect(header("apikey", "test-service-key"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertEquals(List.of(), service.findMeeting("user-a", "meeting-1"));
        server.verify();
    }

    @Test
    void scopesActionItemsToAuthenticatedUserAndMeeting() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SupabaseDataService service = new SupabaseDataService(properties(), builder);
        server.expect(requestTo(containsString("/rest/v1/action_items?")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("meeting_id", equalTo("eq.meeting-1")))
                .andExpect(queryParam("user_id", equalTo("eq.user-a")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertEquals(List.of(), service.findActionItems("user-a", "meeting-1"));
        server.verify();
    }

    private MeetingIqProperties properties() {
        return new MeetingIqProperties(
                "https://supabase.example",
                "test-service-key",
                "test-gemini-key");
    }
}
