package com.ieltspath.user.api.controller;

import com.ieltspath.user.api.dto.request.UserDisplayNamesRequest;
import com.ieltspath.user.api.dto.response.UserDisplayNamesResponse;
import com.ieltspath.user.application.usecase.GetStudyTimezoneUseCase;
import com.ieltspath.user.application.usecase.GetUserDisplayNamesUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Internal user lookups used by other backend services; requests require a verified internal JWT. */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {
    private final GetUserDisplayNamesUseCase displayNames;
    private final GetStudyTimezoneUseCase studyTimezone;

    /** The timezone the learner's study days are counted in; read by the Learning Service. */
    @GetMapping("/{userId}/study-timezone")
    public StudyTimezoneResponse studyTimezone(@PathVariable("userId") UUID userId) {
        return new StudyTimezoneResponse(studyTimezone.execute(userId));
    }

    public record StudyTimezoneResponse(String timezone) {}

    @PostMapping("/display-names")
    public UserDisplayNamesResponse displayNames(@Valid @RequestBody UserDisplayNamesRequest request) {
        return UserDisplayNamesResponse.from(displayNames.execute(request.ids()));
    }
}
