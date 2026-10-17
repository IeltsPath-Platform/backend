package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.dto.request.SchedulePackagePublicationRequest;
import com.ieltspath.content.api.dto.response.PackageVersionPreviewResponse;
import com.ieltspath.content.application.result.ScheduledPackagePublicationResult;
import com.ieltspath.content.application.usecase.CancelPackagePublicationScheduleUseCase;
import com.ieltspath.content.application.usecase.GetPackageVersionPreviewUseCase;
import com.ieltspath.content.application.usecase.ListPackagePublicationSchedulesUseCase;
import com.ieltspath.content.application.usecase.SchedulePackagePublicationUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/content/admin/packages")
@RequiredArgsConstructor
public class ContentPackageAdminController {
    private final GetPackageVersionPreviewUseCase preview;
    private final SchedulePackagePublicationUseCase schedule;
    private final CancelPackagePublicationScheduleUseCase cancel;
    private final ListPackagePublicationSchedulesUseCase listSchedules;

    @GetMapping("/{id}/versions/{versionId}/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public PackageVersionPreviewResponse preview(@PathVariable("id") UUID packageId,
                                                 @PathVariable("versionId") UUID versionId) {
        return PackageVersionPreviewResponse.from(preview.execute(packageId, versionId));
    }

    @PutMapping("/{id}/versions/{versionId}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public ScheduledPackagePublicationResult schedule(@PathVariable("id") UUID packageId,
                                                       @PathVariable("versionId") UUID versionId,
                                                       @Valid @RequestBody SchedulePackagePublicationRequest request) {
        return schedule.execute(packageId, versionId, request.publishAt());
    }

    @DeleteMapping("/{id}/versions/{versionId}/schedule")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public void cancel(@PathVariable("id") UUID packageId, @PathVariable("versionId") UUID versionId) {
        cancel.execute(packageId, versionId);
    }

    @GetMapping("/{id}/publication-schedules")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public List<ScheduledPackagePublicationResult> listSchedules(@PathVariable("id") UUID packageId) {
        return listSchedules.execute(packageId);
    }
}
