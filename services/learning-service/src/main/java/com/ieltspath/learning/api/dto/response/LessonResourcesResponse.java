package com.ieltspath.learning.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ieltspath.learning.application.port.LearningContentClient.LessonResources;
import com.ieltspath.learning.application.port.LearningContentClient.ResourceLink;

import java.util.List;
import java.util.UUID;

/** {@code summaryMarkdown} is always present, as {@code null} when the lesson has no summary. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record LessonResourcesResponse(UUID lessonId, String summaryMarkdown, List<Link> slides, List<Link> documents) {
    public record Link(UUID id, String title, String url, int sortOrder) {
        static Link from(ResourceLink link) {
            return new Link(link.id(), link.title(), link.url(), link.sortOrder());
        }
    }

    public static LessonResourcesResponse from(LessonResources resources) {
        return new LessonResourcesResponse(resources.lessonId(), resources.summaryMarkdown(),
                resources.slides().stream().map(Link::from).toList(),
                resources.documents().stream().map(Link::from).toList());
    }
}
