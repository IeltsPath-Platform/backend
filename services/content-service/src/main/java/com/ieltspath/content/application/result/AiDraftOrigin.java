package com.ieltspath.content.application.result;

import java.util.UUID;

public record AiDraftOrigin(String type, String title, UUID packageId, UUID versionId, UUID sectionId,
                            UUID topicId, UUID lessonId, boolean exists, String knowledgePointName) { }
