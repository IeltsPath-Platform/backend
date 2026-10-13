package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.result.LessonResourceResult;
import com.ieltspath.content.application.result.LessonResourcesResult;
import com.ieltspath.content.domain.vo.LessonResourceType;

import java.util.List;
import java.util.UUID;

/** A lesson's summary with its slides and documents, each in order. Shared by the author and internal reads. */
public record LessonResourcesResponse(UUID lessonId, String summaryMarkdown, List<Item> slides, List<Item> documents) {
    public record Item(UUID id, String title, String url, int sortOrder) {
        static Item from(LessonResourceResult result) {
            return new Item(result.id(), result.title(), result.url(), result.sortOrder());
        }
    }

    public static LessonResourcesResponse from(LessonResourcesResult result) {
        return new LessonResourcesResponse(result.lessonId(), result.summaryMarkdown(),
                items(result, LessonResourceType.SLIDE), items(result, LessonResourceType.DOCUMENT));
    }

    private static List<Item> items(LessonResourcesResult result, LessonResourceType type) {
        return result.resources().stream().filter(resource -> resource.resourceType() == type)
                .map(Item::from).toList();
    }
}
