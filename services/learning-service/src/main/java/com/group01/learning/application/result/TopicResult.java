package com.group01.learning.application.result;

import com.group01.learning.domain.vo.TopicStatus;
import java.util.UUID;

public record TopicResult(UUID topicId, String code, String name, int sequenceOrder,
                          TopicStatus status, int completedLessonCount) {}
