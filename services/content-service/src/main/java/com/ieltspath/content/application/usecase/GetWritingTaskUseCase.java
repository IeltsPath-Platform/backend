package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.port.WritingTaskReader;
import com.ieltspath.content.domain.exception.PackageVersionNotFoundException;
import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.MediaReferencePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** The prompt of a writing task for the practice workspace; the answer spec and chart facts are never part of it. */
@Service
public class GetWritingTaskUseCase {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final WritingTaskReader reader;
    private final MediaReferencePolicy mediaPolicy;

    public GetWritingTaskUseCase(WritingTaskReader reader, MediaReferencePolicy mediaPolicy) {
        this.reader = reader;
        this.mediaPolicy = mediaPolicy;
    }

    @Transactional(readOnly = true)
    public Result execute(UUID packageVersionId, UUID sectionId) {
        WritingTaskReader.WritingTask task = reader.find(packageVersionId, sectionId)
                .orElseThrow(() -> new PackageVersionNotFoundException(packageVersionId));
        List<Image> images = task.images().stream()
                .map(image -> new Image(mediaPolicy.resolve(AssetType.IMAGE, image.mediaReference()), image.altText()))
                .toList();
        return new Result(task, images, parse(task.guideSteps()));
    }

    private static JsonNode parse(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return JSON.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored writing guide steps are unreadable", exception);
        }
    }

    public record Image(String url, String altText) {}

    public record Result(WritingTaskReader.WritingTask task, List<Image> images, JsonNode guideSteps) {}
}
