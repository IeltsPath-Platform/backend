package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.dto.request.SectionMediaRequests;
import com.ieltspath.content.api.dto.response.AuthorPackageResponse;
import com.ieltspath.content.application.usecase.SectionMediaUseCases;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/** Passage and audio of a section in a draft version. */
@RestController
@RequestMapping("/api/content/admin/packages/{packageId}/versions/{versionId}/sections/{sectionId}")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class SectionMediaController {

    private final SectionMediaUseCases media;

    public SectionMediaController(SectionMediaUseCases media) {
        this.media = media;
    }

    @PutMapping("/passage")
    public Map<String, String> setPassage(@PathVariable("packageId") UUID packageId,
                                          @PathVariable("versionId") UUID versionId,
                                          @PathVariable("sectionId") UUID sectionId,
                                          @Valid @RequestBody SectionMediaRequests.Passage request) {
        return Map.of("passage", media.setPassage(packageId, versionId, sectionId, request.text()));
    }

    @DeleteMapping("/passage")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearPassage(@PathVariable("packageId") UUID packageId, @PathVariable("versionId") UUID versionId,
                             @PathVariable("sectionId") UUID sectionId) {
        media.clearPassage(packageId, versionId, sectionId);
    }

    @PutMapping("/audio")
    public AuthorPackageResponse.Audio setAudio(@PathVariable("packageId") UUID packageId,
                                                @PathVariable("versionId") UUID versionId,
                                                @PathVariable("sectionId") UUID sectionId,
                                                @Valid @RequestBody SectionMediaRequests.Audio request) {
        return AuthorPackageResponse.Audio.from(media.setAudio(packageId, versionId, sectionId,
                request.mediaReference(), request.durationSeconds(), request.transcript()));
    }

    @DeleteMapping("/audio")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearAudio(@PathVariable("packageId") UUID packageId, @PathVariable("versionId") UUID versionId,
                           @PathVariable("sectionId") UUID sectionId) {
        media.clearAudio(packageId, versionId, sectionId);
    }
}
