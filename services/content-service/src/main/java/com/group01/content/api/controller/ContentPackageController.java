package com.group01.content.api.controller;

import com.group01.content.api.AccessLevelCompatibility;
import com.group01.content.api.dto.AccessLevel;
import com.group01.content.api.dto.request.AddContentSectionRequest;
import com.group01.content.api.dto.request.AddPackageVersionRequest;
import com.group01.content.api.dto.request.CreateContentPackageRequest;
import com.group01.content.api.dto.request.PublishPackageRequest;
import com.group01.content.api.dto.response.ContentPackageDetailResponse;
import com.group01.content.api.dto.response.ContentPackageResponse;
import com.group01.content.api.dto.response.ContentSectionResponse;
import com.group01.content.application.command.AddContentSectionCommand;
import com.group01.content.application.command.AddPackageVersionCommand;
import com.group01.content.application.command.CreateContentPackageCommand;
import com.group01.content.application.command.PublishContentPackageCommand;
import com.group01.content.application.result.ContentPackageDetailResult;
import com.group01.content.application.result.ContentPackageResult;
import com.group01.content.application.result.ContentSectionResult;
import com.group01.content.application.usecase.*;
import com.group01.content.domain.vo.PublicationStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/content/packages")
@RequiredArgsConstructor
public class ContentPackageController {

    private final ListContentPackagesUseCase listContentPackagesUseCase;
    private final GetContentPackageDetailUseCase getContentPackageDetailUseCase;
    private final CreateContentPackageUseCase createContentPackageUseCase;
    private final AddPackageVersionUseCase addPackageVersionUseCase;
    private final AddContentSectionUseCase addContentSectionUseCase;
    private final PublishContentPackageUseCase publishContentPackageUseCase;

    @GetMapping
    public List<ContentPackageResponse> listPackages(
            @RequestParam(value = "accessLevel", required = false) AccessLevel accessLevel,
            @RequestParam(value = "status", required = false) PublicationStatus status
    ) {
        return listContentPackagesUseCase.execute(
                        AccessLevelCompatibility.toFeatureRequiredFilter(accessLevel), status
                ).stream()
                .map(ContentPackageResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ContentPackageDetailResponse getPackageDetail(@PathVariable("id") UUID id) {
        ContentPackageDetailResult result = getContentPackageDetailUseCase.execute(id);
        return ContentPackageDetailResponse.from(result);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContentPackageResponse createPackage(@Valid @RequestBody CreateContentPackageRequest request) {
        ContentPackageResult result = createContentPackageUseCase.execute(new CreateContentPackageCommand(
                request.code(),
                request.title(),
                request.packageType(),
                AccessLevelCompatibility.toRequiredFeatureKey(request.accessLevel(), "PREMIUM_CONTENT")
        ));
        return ContentPackageResponse.from(result);
    }

    @PostMapping("/{id}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentPackageResponse addVersion(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AddPackageVersionRequest request
    ) {
        ContentPackageResult result = addPackageVersionUseCase.execute(new AddPackageVersionCommand(
                id,
                request.versionNumber(),
                request.rulesJson()
        ));
        return ContentPackageResponse.from(result);
    }

    @PostMapping("/versions/{versionId}/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentSectionResponse addSection(
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody AddContentSectionRequest request
    ) {
        ContentSectionResult result = addContentSectionUseCase.execute(new AddContentSectionCommand(
                versionId,
                request.title(),
                request.skill(),
                request.sortOrder(),
                request.timeLimitSeconds(),
                request.instructions()
        ));
        return ContentSectionResponse.from(result);
    }

    @PostMapping("/{id}/publish")
    public ContentPackageResponse publishPackage(
            @PathVariable("id") UUID id,
            @Valid @RequestBody PublishPackageRequest request
    ) {
        ContentPackageResult result = publishContentPackageUseCase.execute(new PublishContentPackageCommand(
                id,
                request.versionId(),
                null
        ));
        return ContentPackageResponse.from(result);
    }
}
