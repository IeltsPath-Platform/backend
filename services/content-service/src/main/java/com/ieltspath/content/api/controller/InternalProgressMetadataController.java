package com.ieltspath.content.api.controller;

import com.ieltspath.content.application.result.ProgressMetadataResult;
import com.ieltspath.content.application.usecase.GetProgressMetadataUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/learning-content")
public class InternalProgressMetadataController {
    private final GetProgressMetadataUseCase metadata;
    public InternalProgressMetadataController(GetProgressMetadataUseCase metadata) { this.metadata = metadata; }

    @PostMapping("/progress-metadata")
    public ProgressMetadataResult metadata(@Valid @RequestBody Request request) {
        return metadata.execute(request.courseIds());
    }

    public record Request(@NotNull @Size(min = 1, max = 100) List<@NotNull UUID> courseIds) {}
}
