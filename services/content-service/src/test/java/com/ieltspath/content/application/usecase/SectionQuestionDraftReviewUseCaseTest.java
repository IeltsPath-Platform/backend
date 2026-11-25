package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.exception.SectionDraftException;
import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.SectionQuestionRules;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.Skill;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SectionQuestionDraftReviewUseCaseTest {
    private final UUID author = UUID.randomUUID();
    private final UUID draftId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();
    private final UUID packageId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID sectionId = UUID.randomUUID();
    private final AiDraftRepository drafts = mock(AiDraftRepository.class);
    private final ContentPackageRepository packages = mock(ContentPackageRepository.class);
    private final LearningContentReader content = mock(LearningContentReader.class);
    private final CreateQuestionUseCase createQuestion = mock(CreateQuestionUseCase.class);
    private final AddQuestionVersionUseCase addVersion = mock(AddQuestionVersionUseCase.class);
    private final AddSectionQuestionUseCase addToSection = mock(AddSectionQuestionUseCase.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final ObjectMapper json = new ObjectMapper();
    private final ContentPackage pkg = ContentPackage.create("review", "Review", PackageType.PRACTICE_SET, null);
    private final SectionQuestionDraftReviewUseCase useCase = new SectionQuestionDraftReviewUseCase(
            drafts, packages, content, createQuestion, addVersion, addToSection, questions, json,
            new SectionQuestionRules(80, 20_000, 10, .8, .5, 4, 12, 400, 20),
            Clock.fixed(Instant.parse("2026-11-25T00:00:00Z"), ZoneOffset.UTC), mock(EntityManager.class));

    @BeforeEach
    void setUp() {
        when(drafts.findByIdForUpdate(draftId)).thenAnswer(invocation -> Optional.of(readyDraft()));
        when(packages.findByIdForUpdate(packageId)).thenReturn(Optional.of(pkg));
        when(content.packageVersionContent(versionId)).thenReturn(Optional.empty());
    }

    @Test
    void rejectsEditingWithAStaleRevisionWithoutSaving() {
        AiDraft draft = readyDraft();
        when(drafts.findByIdForUpdate(draftId)).thenReturn(Optional.of(draft));

        SectionDraftException failure = assertThrows(SectionDraftException.class,
                () -> useCase.editItem(author, draftId, itemId, 0, json.createObjectNode().put("stem", "changed")));

        assertEquals("DRAFT_CHANGED", failure.code());
        assertEquals(1, failure.details().get("revision"));
        verify(drafts, never()).save(any());
    }

    @Test
    void rejectsAnInvalidEditedQuestionWithoutChangingTheDraft() {
        AiDraft draft = readyDraft();
        when(drafts.findByIdForUpdate(draftId)).thenReturn(Optional.of(draft));
        String original = draft.getResultJson();

        SectionDraftException failure = assertThrows(SectionDraftException.class,
                () -> useCase.editItem(author, draftId, itemId, 1,
                        json.createObjectNode().put("stem", "x".repeat(401))));

        assertEquals("INVALID_DRAFT_ITEM", failure.code());
        assertEquals("BAD_STEM", failure.details().get("reason"));
        assertEquals(original, draft.getResultJson());
        verify(drafts, never()).save(any());
    }

    @Test
    void returnsTheExistingApprovalForTheSameSelectionAndRejectsAnotherSelection() {
        AiDraft draft = approvedDraft();
        when(drafts.findById(draftId)).thenReturn(Optional.of(draft));
        when(drafts.findByIdForUpdate(draftId)).thenReturn(Optional.of(draft));

        assertSame(draft, useCase.approve(author, draftId, List.of(itemId), 1, false));
        SectionDraftException failure = assertThrows(SectionDraftException.class,
                () -> useCase.approve(author, draftId, List.of(UUID.randomUUID()), 1, false));

        assertEquals("DRAFT_ALREADY_REVIEWED", failure.code());
        verifyNoInteractions(createQuestion, addVersion, addToSection);
    }

    @Test
    void refusesToApproveWhenNoItemsWereSelected() {
        AiDraft draft = readyDraft();
        when(drafts.findById(draftId)).thenReturn(Optional.of(draft));
        when(drafts.findByIdForUpdate(draftId)).thenReturn(Optional.of(draft));

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> useCase.approve(author, draftId, List.of(), 1, false));

        assertTrue(failure.getMessage().contains("ít nhất một"));
        verifyNoInteractions(createQuestion, addVersion, addToSection);
    }

    private AiDraft readyDraft() {
        return new AiDraft(draftId, DraftKind.SECTION_QUESTIONS, com.ieltspath.content.domain.vo.DraftStatus.READY,
                author, requestJson(), resultJson(List.of()), null, null, null, Instant.now(), null,
                Instant.now(), null, null, null);
    }

    private AiDraft approvedDraft() {
        return new AiDraft(draftId, DraftKind.SECTION_QUESTIONS, com.ieltspath.content.domain.vo.DraftStatus.APPROVED,
                author, requestJson(), resultJson(List.of(json.createObjectNode().put("itemId", itemId.toString())
                        .put("questionId", UUID.randomUUID().toString()))), null, null, null, Instant.now(), null,
                Instant.now(), author, Instant.now(), null);
    }

    private String requestJson() {
        try {
            return json.writeValueAsString(new RequestSectionQuestionDraftUseCase.StoredRequest(packageId, versionId,
                    sectionId, QuestionType.MULTIPLE_CHOICE, 1, null, Skill.READING, "EXAM", null,
                    new RequestSectionQuestionDraftUseCase.Source("PASSAGE", "A sufficiently long source", "hash")));
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private String resultJson(List<com.fasterxml.jackson.databind.node.ObjectNode> approved) {
        var result = json.createObjectNode().put("revision", 1);
        var items = result.putArray("items");
        var item = items.addObject().put("itemId", itemId.toString()).put("stem", "Which service is available?")
                .put("explanation", "The source mentions the service.");
        item.putArray("options").addObject().put("optionKey", "A").put("content", "Free parking").put("sortOrder", 1);
        item.withArray("options").addObject().put("optionKey", "B").put("content", "A cafe").put("sortOrder", 2);
        item.putObject("answerSpec").put("correct", "A");
        var approvedItems = result.putArray("approved");
        approved.forEach(approvedItems::add);
        try {
            return json.writeValueAsString(result);
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }
}
