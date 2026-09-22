package com.group01.content.api.controller;

import com.group01.content.api.dto.request.CreateContentAssetRequest;
import com.group01.content.api.dto.request.LinkAssetRequest;
import com.group01.content.api.dto.response.ContentAssetResponse;
import com.group01.content.application.command.CreateContentAssetCommand;
import com.group01.content.application.command.LinkAssetCommand;
import com.group01.content.application.result.ContentAssetResult;
import com.group01.content.application.usecase.CreateContentAssetUseCase;
import com.group01.content.application.usecase.GetContentAssetUseCase;
import com.group01.content.application.usecase.LinkAssetUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/content/assets")
@RequiredArgsConstructor
public class ContentAssetController {

    private final GetContentAssetUseCase getContentAssetUseCase;
    private final CreateContentAssetUseCase createContentAssetUseCase;
    private final LinkAssetUseCase linkAssetUseCase;

    @GetMapping("/{id}")
    public ContentAssetResponse getAssetById(@PathVariable("id") UUID id) {
        ContentAssetResult result = getContentAssetUseCase.execute(id);
        return ContentAssetResponse.from(result);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContentAssetResponse createAsset(@Valid @RequestBody CreateContentAssetRequest request) {
        ContentAssetResult result = createContentAssetUseCase.execute(new CreateContentAssetCommand(
                request.assetType(),
                request.textContent(),
                request.mediaReference(),
                request.durationSeconds(),
                request.checksum()
        ));
        return ContentAssetResponse.from(result);
    }

    @PostMapping("/links")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void linkAsset(@Valid @RequestBody LinkAssetRequest request) {
        linkAssetUseCase.execute(new LinkAssetCommand(
                request.assetId(),
                request.sectionId(),
                request.questionVersionId(),
                request.sortOrder()
        ));
    }
}
