package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.AccessLevelCompatibility;
import com.ieltspath.content.api.dto.request.AddContentSectionRequest;
import com.ieltspath.content.api.dto.request.AuthorPackageRequests;
import com.ieltspath.content.api.dto.response.AuthorPackageSummaryResponse;
import com.ieltspath.content.application.command.CreateContentPackageCommand;
import com.ieltspath.content.application.command.SaveContentSectionCommand;
import com.ieltspath.content.application.port.AuthorPackageReader;
import com.ieltspath.content.application.usecase.CreateContentPackageUseCase;
import com.ieltspath.content.application.usecase.ListAuthorPackagesUseCase;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.application.result.AuthorSectionResult;
import com.ieltspath.content.application.result.AuthorVersionResult;
import com.ieltspath.content.application.usecase.AddSectionQuestionUseCase;
import com.ieltspath.content.application.usecase.RemoveAndReorderSectionQuestionsUseCase;
import com.ieltspath.content.api.dto.response.AuthorPackageResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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
    private final AddSectionQuestionUseCase addSectionQuestion;
    private final RemoveAndReorderSectionQuestionsUseCase removeAndReorderQuestions;
    private final ListAuthorPackagesUseCase listPackages;
    private final CreateContentPackageUseCase createPackage;

    @GetMapping
    public List<AuthorPackageSummaryResponse> list(
            @RequestParam(value = "type", required = false) PackageType type,
            @RequestParam(value = "status", required = false) PublicationStatus status,
            @RequestParam(value = "lessonId", required = false) UUID lessonId,
            @RequestParam(value = "topicId", required = false) UUID topicId,
            @RequestParam(value = "courseId", required = false) UUID courseId,
            @RequestParam(value = "q", required = false) String q) {
        return listPackages.execute(new AuthorPackageReader.Filter(type, status, lessonId, topicId, courseId, q))
                .stream().map(AuthorPackageSummaryResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorPackageResponse create(@Valid @RequestBody AuthorPackageRequests.CreatePackage request) {
        return AuthorPackageResponse.from(createPackage.executeAsAuthor(new CreateContentPackageCommand(
                request.code(), request.title(), request.packageType(),
                AccessLevelCompatibility.toRequiredFeatureKey(request.accessLevel(), "PREMIUM_CONTENT"),
                request.lessonId(), request.sourceCode(), request.topicId(), request.courseId())));
    }

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

    @PostMapping("/{packageId}/versions/{versionId}/sections/{sectionId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorPackageResponse.Question addQuestion(@PathVariable("packageId") UUID packageId,
                                                      @PathVariable("versionId") UUID versionId,
                                                      @PathVariable("sectionId") UUID sectionId,
                                                      @Valid @RequestBody AuthorPackageRequests.AddSectionQuestion request) {
        return AuthorPackageResponse.Question.from(addSectionQuestion.execute(packageId, versionId, sectionId,
                request.questionVersionId(), request.maxScore()));
    }

    @DeleteMapping("/{packageId}/versions/{versionId}/sections/{sectionId}/questions/{sectionQuestionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeQuestion(@PathVariable("packageId") UUID packageId,
                               @PathVariable("versionId") UUID versionId,
                               @PathVariable("sectionId") UUID sectionId,
                               @PathVariable("sectionQuestionId") UUID sectionQuestionId) {
        removeAndReorderQuestions.remove(packageId, versionId, sectionId, sectionQuestionId);
    }

    @PutMapping("/{packageId}/versions/{versionId}/sections/{sectionId}/questions/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderQuestions(@PathVariable("packageId") UUID packageId,
                                 @PathVariable("versionId") UUID versionId,
                                 @PathVariable("sectionId") UUID sectionId,
                                 @Valid @RequestBody AuthorPackageRequests.ReorderSectionQuestions request) {
        removeAndReorderQuestions.reorder(packageId, versionId, sectionId, request.sectionQuestionIds());
    }

    private static SaveContentSectionCommand toCommand(UUID packageId, UUID versionId, UUID sectionId,
                                                       AddContentSectionRequest request) {
        return new SaveContentSectionCommand(packageId, versionId, sectionId, request.title(), request.skill(),
                request.sortOrder(), request.timeLimitSeconds(), request.instructions(), request.partNo(),
                request.coverAssetId());
    }
}
