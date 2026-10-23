package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.KnowledgePointLabel;
import com.ieltspath.learning.application.port.LearningContentClient.QuestionLabel;
import com.ieltspath.learning.domain.repository.StaffInsightRepository;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Says what each content work item is about: the knowledge point, question or prerequisite edge, with its name. Names
 * come from Content in one call per list; when Content cannot answer, every subject is {@code null} and the list is
 * still returned. Examiner items have no subject.
 */
@Component
public class WorkItemSubjectResolver {
    private static final Logger log = LoggerFactory.getLogger(WorkItemSubjectResolver.class);

    private final LearningContentClient content;
    private final StaffInsightRepository insight;
    private final ObjectMapper json;

    public WorkItemSubjectResolver(LearningContentClient content, StaffInsightRepository insight, ObjectMapper json) {
        this.content = content;
        this.insight = insight;
        this.json = json;
    }

    /** The subject of each item by item id; an item without a resolvable subject is absent. */
    public Map<UUID, Map<String, Object>> resolve(List<WorkItem> items) {
        Set<UUID> knowledgePoints = new LinkedHashSet<>();
        Set<UUID> questions = new LinkedHashSet<>();
        Set<UUID> gaps = new LinkedHashSet<>();
        Map<UUID, UUID[]> edges = new LinkedHashMap<>();
        for (WorkItem item : items) {
            switch (item.kind()) {
                case CONTENT_GAP -> {
                    knowledgePoints.add(item.subjectRef());
                    gaps.add(item.subjectRef());
                }
                case QUESTION_TOO_EASY, QUESTION_TOO_HARD -> questions.add(item.subjectRef());
                case EDGE_SUSPECT -> {
                    UUID[] ends = edgeEnds(item.detail());
                    if (ends != null) {
                        edges.put(item.id(), ends);
                        knowledgePoints.add(ends[0]);
                        knowledgePoints.add(ends[1]);
                    }
                }
                default -> { }
            }
        }
        if (knowledgePoints.isEmpty() && questions.isEmpty()) {
            return Map.of();
        }

        LearningContentClient.WorkItemSubjects names;
        try {
            names = content.workItemSubjects(knowledgePoints, questions);
        } catch (RuntimeException unavailable) {
            log.warn("Work item subjects not resolved: content did not answer");
            return Map.of();
        }
        Map<UUID, Integer> occurrences = insight.contentGapOccurrences(gaps);

        Map<UUID, Map<String, Object>> subjects = new LinkedHashMap<>();
        for (WorkItem item : items) {
            Map<String, Object> subject = switch (item.kind()) {
                case CONTENT_GAP -> knowledgePoint(names, item.subjectRef(), occurrences.get(item.subjectRef()));
                case QUESTION_TOO_EASY, QUESTION_TOO_HARD -> question(names, item.subjectRef());
                case EDGE_SUSPECT -> edge(names, edges.get(item.id()));
                default -> null;
            };
            if (subject != null) subjects.put(item.id(), subject);
        }
        return subjects;
    }

    private static Map<String, Object> knowledgePoint(LearningContentClient.WorkItemSubjects names, UUID id,
                                                      Integer occurrences) {
        KnowledgePointLabel label = names.knowledgePoints().get(id);
        if (label == null) return null;
        Map<String, Object> subject = new LinkedHashMap<>();
        subject.put("type", "KNOWLEDGE_POINT");
        subject.put("id", id);
        subject.put("code", label.code());
        subject.put("name", label.name());
        subject.put("occurrences", occurrences);
        return subject;
    }

    private static Map<String, Object> question(LearningContentClient.WorkItemSubjects names, UUID questionVersionId) {
        QuestionLabel label = names.questions().get(questionVersionId);
        if (label == null) return null;
        Map<String, Object> subject = new LinkedHashMap<>();
        subject.put("type", "QUESTION");
        subject.put("questionId", label.questionId());
        subject.put("questionVersionId", questionVersionId);
        subject.put("stemPreview", label.stemPreview());
        return subject;
    }

    private static Map<String, Object> edge(LearningContentClient.WorkItemSubjects names, UUID[] ends) {
        if (ends == null) return null;
        KnowledgePointLabel from = names.knowledgePoints().get(ends[0]);
        KnowledgePointLabel to = names.knowledgePoints().get(ends[1]);
        if (from == null || to == null) return null;
        Map<String, Object> subject = new LinkedHashMap<>();
        subject.put("type", "EDGE");
        subject.put("from", end(ends[0], from));
        subject.put("to", end(ends[1], to));
        return subject;
    }

    private static Map<String, Object> end(UUID id, KnowledgePointLabel label) {
        Map<String, Object> end = new LinkedHashMap<>();
        end.put("id", id);
        end.put("code", label.code());
        end.put("name", label.name());
        return end;
    }

    /** The two knowledge points of an edge item's detail, or null when the detail does not carry them. */
    private UUID[] edgeEnds(String detail) {
        try {
            JsonNode node = json.readTree(detail);
            return new UUID[]{UUID.fromString(node.path("from").asText()), UUID.fromString(node.path("to").asText())};
        } catch (Exception malformed) {
            return null;
        }
    }
}
