package com.group01.assessment.domain.entity;

import java.util.UUID;

public record AttemptItem(UUID id, UUID attemptSectionId, UUID questionVersionId, int sortOrder,
                          String questionSnapshot, String answerSnapshot, String knowledgeSnapshot) {}
