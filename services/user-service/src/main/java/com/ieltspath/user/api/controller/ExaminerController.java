package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.user.api.dto.request.SetExaminerCapacityRequest;
import com.ieltspath.user.api.dto.request.UpdateExaminerProfileRequest;
import com.ieltspath.user.api.dto.response.ExaminerAvailabilityResponse;
import com.ieltspath.user.api.dto.response.ExaminerResponse;
import com.ieltspath.user.application.usecase.ExaminerProfilesUseCase;
import com.ieltspath.user.domain.exception.InvalidDataException;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** The examiners a learner chooses from, the examiner's own profile, and the lookup assessment makes. */
@RestController
@RequiredArgsConstructor
public class ExaminerController {
    static final int MAX_BATCH = 50;

    private final ExaminerProfilesUseCase examiners;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Examiners that take new submissions. Any signed-in user may look, on purpose: the learner picks the examiner, so the profile
     * (name, photo, introduction, band, certificates) is public to every signed-in user. It carries no e-mail and no workload
     * limit (docs/contracts/user-internal-v1.md, "The examiner profile is public on purpose").
     */
    @GetMapping("/api/users/examiners")
    public List<ExaminerResponse> list() {
        currentUserProvider.requireUserId();
        return examiners.accepting().stream().map(ExaminerResponse::from).toList();
    }

    @GetMapping("/api/users/examiners/{id}")
    public ExaminerResponse get(@PathVariable("id") UUID id) {
        currentUserProvider.requireUserId();
        return ExaminerResponse.from(examiners.get(id));
    }

    @PutMapping("/api/users/me/examiner-profile")
    @PreAuthorize("hasRole('EXAMINER')")
    public ExaminerResponse updateOwn(@Valid @RequestBody UpdateExaminerProfileRequest request) {
        return ExaminerResponse.from(examiners.updateOwn(currentUserProvider.requireUserId(), request.toCommand()));
    }

    @PutMapping("/api/users/{id}/examiner-profile/capacity")
    @PreAuthorize("hasRole('ADMIN')")
    public ExaminerResponse setCapacity(@PathVariable("id") UUID id, @Valid @RequestBody SetExaminerCapacityRequest request) {
        return ExaminerResponse.from(examiners.setCapacity(id, request.dailyLimit()));
    }

    /** For assessment: is this an active examiner, do they accept submissions, and how many may be open. */
    @Hidden
    @GetMapping("/internal/users/examiners/{id}")
    public ExaminerAvailabilityResponse availability(@PathVariable("id") UUID id) {
        return ExaminerAvailabilityResponse.from(examiners.get(id));
    }

    /**
     * The same for several examiners in one call, so a list needs one request, not one per examiner. Ids that are not active
     * examiners are left out; at most {@value #MAX_BATCH} ids.
     */
    @Hidden
    @GetMapping("/internal/users/examiners")
    public List<ExaminerAvailabilityResponse> availabilityOf(@RequestParam("ids") List<UUID> ids) {
        if (ids.isEmpty() || ids.size() > MAX_BATCH) {
            throw new InvalidDataException("ids must hold 1 to " + MAX_BATCH + " examiner ids");
        }
        return examiners.getAll(ids.stream().distinct().toList()).stream().map(ExaminerAvailabilityResponse::from).toList();
    }
}
