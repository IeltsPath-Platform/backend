package com.group01.assessment.api.dto.response;

import com.group01.assessment.application.result.AttemptStructureResult;

import java.util.List;
import java.util.UUID;

public record AttemptStructureResponse(List<Section> sections) {
    public record Section(UUID id, UUID contentSectionId, int sortOrder, String snapshot, List<Item> items) {}

    public record Item(UUID id, UUID questionVersionId, int sortOrder,
                       String questionSnapshot, String knowledgeSnapshot) {}

    public static AttemptStructureResponse from(AttemptStructureResult result) {
        return new AttemptStructureResponse(result.sections().stream()
                .map(section -> new Section(
                        section.id(),
                        section.contentSectionId(),
                        section.sortOrder(),
                        section.snapshot(),
                        section.items().stream()
                                .map(item -> new Item(
                                        item.id(),
                                        item.questionVersionId(),
                                        item.sortOrder(),
                                        item.questionSnapshot(),
                                        item.knowledgeSnapshot()))
                                .toList()))
                .toList());
    }
}
