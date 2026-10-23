package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.KnowledgePointLabel;
import com.ieltspath.learning.application.port.LearningContentClient.QuestionLabel;
import com.ieltspath.learning.application.port.LearningContentClient.WorkItemSubjects;
import com.ieltspath.learning.domain.repository.StaffInsightRepository;
import com.ieltspath.learning.domain.vo.StaffRecords.Queue;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkItem;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkKind;
import com.ieltspath.learning.domain.vo.StaffRecords.WorkStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkItemSubjectResolverTest {

    private final LearningContentClient content = mock(LearningContentClient.class);
    private final StaffInsightRepository insight = mock(StaffInsightRepository.class);
    private final WorkItemSubjectResolver resolver = new WorkItemSubjectResolver(content, insight, new ObjectMapper());

    private final UUID kp = UUID.randomUUID();
    private final UUID from = UUID.randomUUID();
    private final UUID to = UUID.randomUUID();
    private final UUID questionVersion = UUID.randomUUID();
    private final UUID question = UUID.randomUUID();

    private static WorkItem item(WorkKind kind, UUID subjectRef, String detail) {
        return new WorkItem(UUID.randomUUID(), kind == WorkKind.LEARNER_FLAG ? Queue.EXAMINER : Queue.CONTENT_AUTHOR,
                kind, null, subjectRef, detail, WorkStatus.OPEN, null, null, null, null, false, Instant.now(),
                Instant.now());
    }

    private void contentKnows() {
        when(content.workItemSubjects(any(), any())).thenReturn(new WorkItemSubjects(
                Map.of(kp, new KnowledgePointLabel("R-PARA", "Paraphrase"),
                        from, new KnowledgePointLabel("R-A", "Skimming"), to, new KnowledgePointLabel("R-B", "Scanning")),
                Map.of(questionVersion, new QuestionLabel(question, "Which sentence best…"))));
    }

    @Test
    void everyKindGetsItsOwnSubject() {
        contentKnows();
        when(insight.contentGapOccurrences(any())).thenReturn(Map.of(kp, 7));
        WorkItem gap = item(WorkKind.CONTENT_GAP, kp, "{}");
        WorkItem easy = item(WorkKind.QUESTION_TOO_EASY, questionVersion, "{}");
        WorkItem hard = item(WorkKind.QUESTION_TOO_HARD, questionVersion, "{}");
        WorkItem edge = item(WorkKind.EDGE_SUSPECT, UUID.randomUUID(),
                "{\"from\":\"" + from + "\",\"to\":\"" + to + "\",\"probes\":5}");

        Map<UUID, Map<String, Object>> subjects = resolver.resolve(List.of(gap, easy, hard, edge));

        Map<String, Object> gapSubject = subjects.get(gap.id());
        assertEquals("KNOWLEDGE_POINT", gapSubject.get("type"));
        assertEquals(kp, gapSubject.get("id"));
        assertEquals("R-PARA", gapSubject.get("code"));
        assertEquals("Paraphrase", gapSubject.get("name"));
        assertEquals(7, gapSubject.get("occurrences"));

        for (WorkItem question : List.of(easy, hard)) {
            Map<String, Object> subject = subjects.get(question.id());
            assertEquals("QUESTION", subject.get("type"));
            assertEquals(this.question, subject.get("questionId"));
            assertEquals(questionVersion, subject.get("questionVersionId"));
            assertEquals("Which sentence best…", subject.get("stemPreview"));
        }

        Map<String, Object> edgeSubject = subjects.get(edge.id());
        assertEquals("EDGE", edgeSubject.get("type"));
        assertEquals("Skimming", ((Map<?, ?>) edgeSubject.get("from")).get("name"));
        assertEquals(to, ((Map<?, ?>) edgeSubject.get("to")).get("id"));
    }

    @Test
    void contentIsAskedOnceForTheWholeList() {
        contentKnows();
        when(insight.contentGapOccurrences(any())).thenReturn(Map.of());

        resolver.resolve(List.of(item(WorkKind.CONTENT_GAP, kp, "{}"), item(WorkKind.CONTENT_GAP, from, "{}"),
                item(WorkKind.QUESTION_TOO_EASY, questionVersion, "{}"),
                item(WorkKind.EDGE_SUSPECT, UUID.randomUUID(), "{\"from\":\"" + from + "\",\"to\":\"" + to + "\"}")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UUID>> points = ArgumentCaptor.forClass(Collection.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UUID>> questions = ArgumentCaptor.forClass(Collection.class);
        verify(content, times(1)).workItemSubjects(points.capture(), questions.capture());
        assertEquals(java.util.Set.of(kp, from, to), new java.util.HashSet<>(points.getValue()));
        assertEquals(java.util.Set.of(questionVersion), new java.util.HashSet<>(questions.getValue()));
    }

    @Test
    void whenContentCannotAnswerThereAreNoSubjects() {
        when(content.workItemSubjects(any(), any())).thenThrow(new IllegalStateException("down"));

        Map<UUID, Map<String, Object>> subjects = resolver.resolve(List.of(item(WorkKind.CONTENT_GAP, kp, "{}")));

        assertTrue(subjects.isEmpty());
    }

    @Test
    void anUnknownIdHasNoSubject() {
        when(content.workItemSubjects(any(), any())).thenReturn(new WorkItemSubjects(Map.of(), Map.of()));
        when(insight.contentGapOccurrences(any())).thenReturn(Map.of());
        WorkItem gap = item(WorkKind.CONTENT_GAP, kp, "{}");

        assertNull(resolver.resolve(List.of(gap)).get(gap.id()));
    }

    @Test
    void examinerItemsNeverCallContent() {
        Map<UUID, Map<String, Object>> subjects = resolver.resolve(List.of(
                item(WorkKind.LEARNER_FLAG, UUID.randomUUID(), "{}")));

        assertTrue(subjects.isEmpty());
        verify(content, never()).workItemSubjects(any(), any());
    }

    @Test
    void anEdgeWithUnreadableDetailHasNoSubjectButTheListStillResolves() {
        contentKnows();
        when(insight.contentGapOccurrences(any())).thenReturn(Map.of());
        WorkItem broken = item(WorkKind.EDGE_SUSPECT, UUID.randomUUID(), "not json");
        WorkItem gap = item(WorkKind.CONTENT_GAP, kp, "{}");

        Map<UUID, Map<String, Object>> subjects = resolver.resolve(List.of(broken, gap));

        assertNull(subjects.get(broken.id()));
        assertEquals("Paraphrase", subjects.get(gap.id()).get("name"));
    }
}
