package com.ieltspath.library.application.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TopicProgressResult(List<Topic> topics, Current current) {
    public record Topic(UUID topicId, String name, long total, long learned, int percent, String status) {}
    public record Current(UUID topicId, String name, Instant lastReviewedAt, List<NextWord> nextWords) {}
    public record NextWord(UUID senseId, UUID itemId, String lemma, String vietnameseMeaning) {}
}
