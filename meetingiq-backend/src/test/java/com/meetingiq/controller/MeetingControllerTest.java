package com.meetingiq.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.meetingiq.dto.ExtractionResponse;
import com.meetingiq.dto.MeetingResponse;
import com.meetingiq.exception.ApiExceptionHandler;
import com.meetingiq.exception.InvalidRequestException;
import com.meetingiq.service.MeetingService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(MeetingController.class)
@Import(ApiExceptionHandler.class)
class MeetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeetingService meetingService;

    @Test
    void returnsMeetingHistoryWithSnakeCaseContract() throws Exception {
        when(meetingService.getMeetings("Bearer valid-token"))
                .thenReturn(List.of(new MeetingResponse(
                        "meeting-1",
                        "Planning",
                        "2026-10-24",
                        "2026-10-24T10:00:00Z",
                        List.of(),
                        List.of("Budget approved"))));

        mockMvc.perform(get("/meetings").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].meeting_id").value("meeting-1"))
                .andExpect(jsonPath("$[0].meeting_date").value("2026-10-24"))
                .andExpect(jsonPath("$[0].action_items").isArray())
                .andExpect(jsonPath("$[0].key_points[0]").value("Budget approved"));
    }

    @Test
    void returnsUnauthorizedForMissingToken() throws Exception {
        when(meetingService.getMeetings(null))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing auth token"));

        mockMvc.perform(get("/meetings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Missing auth token"));
    }

    @Test
    void returnsNotFoundWhenMeetingIsUnavailableToUser() throws Exception {
        when(meetingService.getMeeting("Bearer valid-token", "other-user-meeting"))
                .thenThrow(new com.meetingiq.exception.ResourceNotFoundException("Meeting not found"));

        mockMvc.perform(get("/meetings/other-user-meeting").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Meeting not found"));
    }

    @Test
    void validatesTranscriptRequest() throws Exception {
        mockMvc.perform(post("/extract")
                        .header("Authorization", "Bearer valid-token")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                        .andExpect(jsonPath("$.detail[0].loc[0]").value("body"))
                        .andExpect(jsonPath("$.detail[0].loc[1]").value("transcript"));
    }

    @Test
    void rejectsEmptyAudioUpload() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.webm",
                "audio/webm",
                new byte[0]);
        when(meetingService.extractAudio(null, new byte[0], "audio/webm", "empty.webm"))
                .thenThrow(new InvalidRequestException("Audio file must not be empty"));

        mockMvc.perform(multipart("/extract-audio").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Audio file must not be empty"));
    }

    @Test
    void authenticatesAudioUploadBeforeValidatingTheFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.webm",
                "audio/webm",
                new byte[0]);
        when(meetingService.extractAudio(null, new byte[0], "audio/webm", "empty.webm"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing auth token"));

        mockMvc.perform(multipart("/extract-audio").file(emptyFile))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Missing auth token"));
    }

    @Test
    void passesAudioUploadThroughToService() throws Exception {
        MockMultipartFile audioFile = new MockMultipartFile(
                "file",
                "meeting.webm",
                "audio/webm",
                new byte[] {1, 2, 3});
        when(meetingService.extractAudio(any(), any(), any(), any()))
                .thenReturn(new ExtractionResponse("meeting-1", "Planning", "2026-10-24", List.of(), List.of()));

        mockMvc.perform(multipart("/extract-audio")
                        .file(audioFile)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meeting_id").value("meeting-1"));

        verify(meetingService).extractAudio("Bearer valid-token", new byte[] {1, 2, 3}, "audio/webm", "meeting.webm");
    }
}
