package com.group01.assessment.domain.entity;

import java.util.UUID;

public record AttemptSection(UUID id, UUID attemptId, UUID contentSectionId, int sortOrder, String snapshot) {}
