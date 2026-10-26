package com.ieltspath.content.application.result;

import com.ieltspath.content.application.port.CourseCandidateReader.PracticeSetRef;

import java.util.List;
import java.util.UUID;

/**
 * What the course generator is allowed to use: knowledge points in teaching order (every prerequisite before the
 * points that need it) with the practice sets and questions available for each. {@code unmatchedTopics} are the phrases
 * that matched no knowledge point.
 */
public record CourseCandidates(List<Candidate> points, List<String> unmatchedTopics) {

    public CourseCandidates {
        points = List.copyOf(points);
        unmatchedTopics = List.copyOf(unmatchedTopics);
    }

    /** {@code foundation} points support the requested ones and may stay out of the lessons. */
    public record Candidate(UUID id, String code, String name, String description, boolean foundation,
                            List<PracticeSetRef> practiceSets, List<UUID> questions) {
        public Candidate {
            practiceSets = List.copyOf(practiceSets);
            questions = List.copyOf(questions);
        }
    }
}
