package com.meetingiq.controller;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.meetingiq.dto.ExtractionResponse;
import com.meetingiq.dto.MeetingResponse;
import com.meetingiq.dto.TranscriptRequest;
import com.meetingiq.exception.InvalidRequestException;
import com.meetingiq.service.MeetingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping
public class MeetingController {

    private static final Map<String, String> AUDIO_MIME_TYPES = Map.ofEntries(
            Map.entry("aac", "audio/aac"),
            Map.entry("aif", "audio/aiff"),
            Map.entry("aiff", "audio/aiff"),
            Map.entry("flac", "audio/flac"),
            Map.entry("m4a", "audio/mp4"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("mpeg", "video/mpeg"),
            Map.entry("mpg", "video/mpeg"),
            Map.entry("oga", "audio/ogg"),
            Map.entry("ogg", "audio/ogg"),
            Map.entry("opus", "audio/ogg"),
            Map.entry("wav", "audio/wav"),
            Map.entry("webm", "audio/webm"),
            Map.entry("3gp", "video/3gpp"),
            Map.entry("3gpp", "video/3gpp"),
            Map.entry("avi", "video/x-msvideo"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("wmv", "video/x-ms-wmv"));

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @GetMapping("/meetings")
    public List<MeetingResponse> getMeetings(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return meetingService.getMeetings(authorization);
    }

    @GetMapping("/meetings/{meetingId}")
    public MeetingResponse getMeeting(
            @PathVariable String meetingId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return meetingService.getMeeting(authorization, meetingId);
    }

    @PostMapping("/extract")
    public ExtractionResponse extractTranscript(
            @Valid @RequestBody TranscriptRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return meetingService.extractTranscript(authorization, request.transcript());
    }

    @PostMapping(value = "/extract-audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExtractionResponse extractAudio(
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String mimeType = resolveMimeType(file);
        try {
            return meetingService.extractAudio(
                    authorization,
                    file.getBytes(),
                    mimeType,
                    file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename());
        } catch (java.io.IOException exception) {
            throw new InvalidRequestException("Unable to read uploaded audio file");
        }
    }

    private String resolveMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String normalized = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("audio/") || normalized.startsWith("video/")) {
                return normalized;
            }
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return AUDIO_MIME_TYPES.get(extension);
    }
}
