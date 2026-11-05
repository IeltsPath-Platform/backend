package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.entity.TopicProgress;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.TopicStatus;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

public final class TopicStatusDeriver {
    public Map<UUID, TopicStatus> derive(List<TopicProgress> topics) {
        List<TopicProgress> ordered = topics.stream()
                .filter(topic -> topic.sequenceOrder() != null)
                .sorted(Comparator.comparing(TopicProgress::sequenceOrder)
                        .thenComparing(topic -> topic.topicId().toString()))
                .toList();
        Map<UUID, TopicStatus> statuses = new LinkedHashMap<>();
        Set<ProgressionGroup> openGroups = new HashSet<>();
        for (TopicProgress topic : ordered) {
            TopicStatus status;
            if (topic.passedAt() != null) {
                status = TopicStatus.PASSED;
            } else if (topic.testedAt() != null) {
                status = TopicStatus.COMPLETED;
            } else {
                Set<ProgressionGroup> groups = groupsFor(topic);
                status = groups.stream().anyMatch(openGroups::contains)
                        ? TopicStatus.LOCKED : TopicStatus.IN_PROGRESS;
                openGroups.addAll(groups);
            }
            statuses.put(topic.topicId(), status);
        }
        return Collections.unmodifiableMap(statuses);
    }

    private Set<ProgressionGroup> groupsFor(TopicProgress topic) {
        if (topic.courseId() == null) return Set.of(new ProgressionGroup(null, null));
        if (topic.skills().isEmpty()) return Set.of(new ProgressionGroup(topic.courseId(), null));
        return topic.skills().stream()
                .map(skill -> new ProgressionGroup(topic.courseId(), skill))
                .collect(Collectors.toSet());
    }

    private record ProgressionGroup(UUID courseId, LearningSkill skill) {}
}
