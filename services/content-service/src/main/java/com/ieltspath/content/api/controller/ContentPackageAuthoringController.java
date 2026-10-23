package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.dto.request.AddContentSectionRequest;
import com.ieltspath.content.api.dto.request.AuthorPackageRequests;
import com.ieltspath.content.api.dto.response.AuthorPackageResponse;
import com.ieltspath.content.application.command.SaveContentSectionCommand;
import com.ieltspath.content.application.result.AuthorSectionResult;
import com.ieltspath.content.application.result.AuthorVersionResult;
import com.ieltspath.content.application.usecase.CreateContentSectionUseCase;
import com.ieltspath.content.application.usecase.CreateDraftPackageVersionUseCase;
import com.ieltspath.content.application.usecase.DeleteContentSectionUseCase;
import com.ieltspath.content.application.usecase.GetAuthorPackageUseCase;
import com.ieltspath.content.application.usecase.ReorderContentSectionsUseCase;
import com.ieltspath.content.application.usecase.UpdateContentSectionUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Author routes for building a package. Every write is refused unless the target version is a draft. */
@RestController
@RequestMapping("/api/content/admin/packages")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class ContentPackageAuthoringController {

    private final GetAuthorPackageUseCase getPackage;
    private final CreateDraftPackageVersionUseCase createDraft;
    private final CreateContentSectionUseCase createSection;
    private final UpdateContentSectionUseCase updateSection;
    private final DeleteContentSectionUseCase deleteSection;
    private final ReorderContentSectionsUseCase reorderSections;

    @GetMapping("/{packageId}")
    public AuthorPackageResponse get(@PathVariable("packageId") UUID packageId) {
        return AuthorPackageResponse.from(getPackage.execute(packageId));
    }

    @PostMapping("/{packageId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorPackageResponse.Version createDraftVersion(@PathVariable("packageId") UUID packageId,
                                                            @Valid @RequestBody(required = false)
                                                            AuthorPackageRequests.CreateDraftVersion request) {
        String rulesJson = request == null ? null : request.rulesJson();
        AuthorVersionResult version = createDraft.execute(packageId, rulesJson);
        return AuthorPackageResponse.Version.from(version);
    }

    @PostMapping("/{packageId}/versions/{versionId}/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorPackageResponse.Section createSection(@PathVariable("packageId") UUID packageId, @PathVariable("versionId") UUID versionId,
                                                       @Valid @RequestBody AddContentSectionRequest request) {
        AuthorSectionResult section = createSection.execute(toCommand(packageId, versionId, null, request));
        return AuthorPackageResponse.Section.from(section);
    }

    @PutMapping("/{packageId}/versions/{versionId}/sections/{sectionId}")
    public AuthorPackageResponse.Section updateSection(@PathVariable("packageId") UUID packageId, @PathVariable("versionId") UUID versionId,
                                                       @PathVariable("sectionId") UUID sectionId,
                                                       @Valid @RequestBody AddContentSectionRequest request) {
        AuthorSectionResult section = updateSection.execute(toCommand(packageId, versionId, sectionId, request));
        return AuthorPackageResponse.Section.from(section);
    }

    @DeleteMapping("/{packageId}/versions/{versionId}/sections/{sectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSection(@PathVariable("packageId") UUID packageId, @PathVariable("versionId") UUID versionId,
                              @PathVariable("sectionId") UUID sectionId) {
        deleteSection.execute(packageId, versionId, sectionId);
    }

    @PutMapping("/{packageId}/versions/{versionId}/sections/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderSections(@PathVariable("packageId") UUID packageId, @PathVariable("versionId") UUID versionId,
                                @Valid @RequestBody AuthorPackageRequests.ReorderSections request) {
        reorderSections.execute(packageId, versionId, request.sectionIds());
    }

    private static SaveContentSectionCommand toCommand(UUID packageId, UUID versionId, UUID sectionId,
                                                       AddContentSectionRequest request) {
        return new SaveContentSectionCommand(packageId, versionId, sectionId, request.title(), request.skill(),
                request.sortOrder(), request.timeLimitSeconds(), request.instructions(), request.partNo(),
                request.coverAssetId());
    }
}
