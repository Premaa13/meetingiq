package com.meetingiq.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meetingiq.SupabaseAuthService;
import com.meetingiq.dto.ExtractedActionItem;
import com.meetingiq.dto.ExtractionResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    private SupabaseAuthService authService;

    @Mock
    private SupabaseDataService dataService;

    @Mock
    private GeminiExtractionService extractionService;

    @InjectMocks
    private MeetingService meetingService;

    @Test
    void extractsAndPersistsTranscriptUsingAuthenticatedUserAndExistingRelations() {
        String transcript = "The team agreed to publish the agenda by Friday.";
        List<ExtractedActionItem> actions = List.of(
                new ExtractedActionItem("Publish the agenda", "Ari", "Friday", "2026-10-30", "high"));
        ExtractionResult extraction = new ExtractionResult(
                "Weekly planning",
                "2026-10-24",
                List.of("Agenda approved"),
                actions);
        when(authService.getAuthenticatedUserId("Bearer user-token")).thenReturn("user-123");
        when(extractionService.extractTranscript(transcript)).thenReturn(extraction);
        when(dataService.insertMeeting("user-123", "Weekly planning", "2026-10-24", transcript))
                .thenReturn("meeting-456");

        var response = meetingService.extractTranscript("Bearer user-token", transcript);

        assertEquals("meeting-456", response.meetingId());
        assertEquals("Weekly planning", response.title());
        assertEquals(actions, response.actionItems());
        verify(dataService).insertKeyPoints("user-123", "meeting-456", List.of("Agenda approved"));
        verify(dataService).insertActionItems("user-123", "meeting-456", actions);
    }

    @Test
    void recordsOriginalFilenameForAudioMeetingTranscript() {
        byte[] audio = {1, 2, 3};
        ExtractionResult extraction = new ExtractionResult("Team sync", "2026-10-24", List.of(), List.of());
        when(authService.getAuthenticatedUserId("Bearer user-token")).thenReturn("user-123");
        when(extractionService.extractAudio(audio, "audio/webm")).thenReturn(extraction);
        when(dataService.insertMeeting(
                "user-123",
                "Team sync",
                "2026-10-24",
                "[Audio recording: meeting.webm]"))
                .thenReturn("meeting-789");

        var response = meetingService.extractAudio("Bearer user-token", audio, "audio/webm", "meeting.webm");

        assertEquals("meeting-789", response.meetingId());
        verify(dataService).insertKeyPoints("user-123", "meeting-789", List.of());
        verify(dataService).insertActionItems("user-123", "meeting-789", List.of());
    }
}
