package com.group01.learning.domain.service;

import com.group01.learning.domain.entity.TopicProgress;
import com.group01.learning.domain.vo.TopicStatus;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

public final class TopicStatusDeriver {
    public Map<UUID, TopicStatus> derive(List<TopicProgress> topics) {
        List<TopicProgress> ordered = topics.stream()
                .filter(topic -> topic.sequenceOrder() != null)
                .sorted(Comparator.comparing(TopicProgress::sequenceOrder)
                        .thenComparing(topic -> topic.topicId().toString()))
                .toList();
        Map<UUID, TopicStatus> statuses = new LinkedHashMap<>();
        Set<UUID> openCourses = new HashSet<>();
        for (TopicProgress topic : ordered) {
            TopicStatus status;
            if (topic.passedAt() != null) {
                status = TopicStatus.PASSED;
            } else if (openCourses.add(topic.courseId())) {
                status = TopicStatus.IN_PROGRESS;
            } else {
                status = TopicStatus.LOCKED;
            }
            statuses.put(topic.topicId(), status);
        }
        return Collections.unmodifiableMap(statuses);
    }
}
