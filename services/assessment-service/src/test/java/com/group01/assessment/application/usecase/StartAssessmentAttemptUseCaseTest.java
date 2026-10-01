package com.group01.assessment.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.assessment.application.command.StartAssessmentAttemptCommand;
import com.group01.assessment.application.exception.ContentUnavailableException;
import com.group01.assessment.application.port.ContentPackageProvider;
import com.group01.assessment.application.port.ContentPackageProvider.Item;
import com.group01.assessment.application.port.ContentPackageProvider.KnowledgePointWeight;
import com.group01.assessment.application.port.ContentPackageProvider.Option;
import com.group01.assessment.application.port.ContentPackageProvider.PackageVersion;
import com.group01.assessment.application.port.ContentPackageProvider.Section;
import com.group01.assessment.domain.aggregate.AssessmentAttempt;
import com.group01.assessment.domain.entity.AttemptItem;
import com.group01.assessment.domain.entity.AttemptItemKnowledgePoint;
import com.group01.assessment.domain.entity.AttemptSection;
import com.group01.assessment.domain.exception.AssessmentNotFoundException;
import com.group01.assessment.domain.exception.PackageNotAttemptableException;
import com.group01.assessment.domain.repository.*;
import com.group01.assessment.domain.vo.AttemptChannel;
import com.group01.assessment.domain.vo.AttemptMode;
import com.group01.assessment.domain.vo.AttemptType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartAssessmentAttemptUseCaseTest {
    @Mock ContentPackageProvider content;
    @Mock AssessmentAttemptRepository attempts;
    @Mock AttemptSectionRepository sections;
    @Mock AttemptItemRepository items;
    @Mock AttemptItemKnowledgePointRepository knowledgeSnapshot;

    private final ObjectMapper json = new ObjectMapper();
    private final UUID userId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID questionVersionId = UUID.randomUUID();
    private final UUID knowledgePointId = UUID.randomUUID();

    @Test
    @SuppressWarnings("unchecked")
    void topicTestBecomesTopicGateAndFreezesTheAnswerAwayFromTheQuestion() throws Exception {
        when(content.findPackageVersion(versionId)).thenReturn(Optional.of(version("TOPIC_TEST", "Paragraph A.")));

        var started = useCase().execute(command());

        assertEquals(AttemptType.TOPIC_GATE, started.attemptType());
        assertNull(started.expiresAt());
        ArgumentCaptor<AssessmentAttempt> attempt = ArgumentCaptor.forClass(AssessmentAttempt.class);
        verify(attempts).save(attempt.capture());
        assertNull(attempt.getValue().getLearningGoalId());

        ArgumentCaptor<List<AttemptSection>> savedSections = ArgumentCaptor.forClass(List.class);
        verify(sections).saveAll(savedSections.capture());
        JsonNode section = json.readTree(savedSections.getValue().getFirst().snapshot());
        assertEquals("Street trees", section.get("title").asText());
        assertEquals("READING", section.get("skill").asText());
        assertEquals("Paragraph A.", section.get("passage").asText());

        ArgumentCaptor<List<AttemptItem>> savedItems = ArgumentCaptor.forClass(List.class);
        verify(items).saveAll(savedItems.capture());
        AttemptItem item = savedItems.getValue().getFirst();
        JsonNode question = json.readTree(item.questionSnapshot());
        assertEquals("What is the passage mainly about?", question.get("stem").asText());
        assertEquals("B", question.get("options").get(1).get("optionKey").asText());
        assertFalse(item.questionSnapshot().contains("answerSpec"));
        assertFalse(item.questionSnapshot().contains("explanation"));
        JsonNode answer = json.readTree(item.answerSnapshot());
        assertEquals("B", answer.get("answerSpec").get("correct").asText());
        assertEquals(1.0, answer.get("maxScore").asDouble());
        assertEquals(knowledgePointId.toString(),
                json.readTree(item.knowledgeSnapshot()).get(0).get("knowledgePointId").asText());

        ArgumentCaptor<List<AttemptItemKnowledgePoint>> snapshot = ArgumentCaptor.forClass(List.class);
        verify(knowledgeSnapshot).saveAll(snapshot.capture());
        assertEquals(List.of(new AttemptItemKnowledgePoint(item.id(), knowledgePointId, BigDecimal.ONE)),
                snapshot.getValue());
    }

    @Test
    void sectionWithoutPassageOmitsTheKey() throws Exception {
        when(content.findPackageVersion(versionId)).thenReturn(Optional.of(version("QUIZ", null)));

        useCase().execute(command());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AttemptSection>> savedSections = ArgumentCaptor.forClass(List.class);
        verify(sections).saveAll(savedSections.capture());
        assertFalse(json.readTree(savedSections.getValue().getFirst().snapshot()).has("passage"));
    }

    @Test
    void practiceSetsAndLessonsCannotBeTakenAsAttempts() {
        for (String type : List.of("PRACTICE_SET", "LESSON")) {
            when(content.findPackageVersion(versionId)).thenReturn(Optional.of(version(type, null)));
            assertThrows(PackageNotAttemptableException.class, () -> useCase().execute(command()));
        }
        verifyNoInteractions(attempts, sections, items, knowledgeSnapshot);
    }

    @Test
    void contentFailureOrUnknownVersionWritesNothing() {
        when(content.findPackageVersion(versionId)).thenThrow(new ContentUnavailableException("down"));
        assertThrows(ContentUnavailableException.class, () -> useCase().execute(command()));

        reset(content);
        when(content.findPackageVersion(versionId)).thenReturn(Optional.empty());
        assertThrows(AssessmentNotFoundException.class, () -> useCase().execute(command()));

        verifyNoInteractions(attempts, sections, items, knowledgeSnapshot);
    }

    private StartAssessmentAttemptCommand command() {
        return new StartAssessmentAttemptCommand(userId, versionId, AttemptMode.STANDARD, AttemptChannel.WEB);
    }

    private PackageVersion version(String type, String passage) {
        Item item = new Item(questionVersionId, 1, "What is the passage mainly about?",
                List.of(new Option("A", "How oak trees grow in cities", 1),
                        new Option("B", "The ways street trees improve city life", 2)),
                Map.of("type", "CHOICE", "correct", "B"), "All three paragraphs describe benefits.", BigDecimal.ONE,
                List.of(new KnowledgePointWeight(knowledgePointId, BigDecimal.ONE)));
        return new PackageVersion(versionId, type, List.of(new Section(UUID.randomUUID(), "Street trees", "READING",
                null, 1, passage, null, List.of(item))));
    }

    private StartAssessmentAttemptUseCase useCase() {
        return new StartAssessmentAttemptUseCase(content,
                new AttemptCreator(attempts, sections, items, knowledgeSnapshot, json));
    }
}
