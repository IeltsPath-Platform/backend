package com.group01.content.api.controller;

import com.group01.content.api.dto.response.ReadingPassageResponse;
import com.group01.content.application.usecase.GetReadingPassageUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/content/reading")
public class ReadingController {

    private final GetReadingPassageUseCase getReadingPassageUseCase;

    public ReadingController(GetReadingPassageUseCase getReadingPassageUseCase) {
        this.getReadingPassageUseCase = getReadingPassageUseCase;
    }

    @GetMapping("/sections/{sectionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public ReadingPassageResponse getReadingPassage(@PathVariable("sectionId") UUID sectionId) {
        return ReadingPassageResponse.from(getReadingPassageUseCase.execute(sectionId));
    }
}
