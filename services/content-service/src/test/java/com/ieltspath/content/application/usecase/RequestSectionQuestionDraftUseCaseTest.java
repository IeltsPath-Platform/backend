package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.command.SectionQuestionDraftRequest;
import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.PackageVersionContentResult;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentSection;
import com.ieltspath.content.domain.exception.SectionDraftOpenException;
import com.ieltspath.content.application.exception.SectionDraftException;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.application.result.SectionQuestionRules;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RequestSectionQuestionDraftUseCaseTest {
    private final ObjectMapper json = new ObjectMapper();
    private final ContentPackage pkg = ContentPackage.create("PKG", "Package", PackageType.PRACTICE_SET, null);
    private final ContentPackageVersion version = pkg.createDraftVersion("{}");
    private final ContentSection section = ContentSection.create(version.getId(), "Reading", Skill.READING, 1, null, "Section directions");
    private final ContentPackageRepository packages = mock(ContentPackageRepository.class);
    private final LearningContentReader content = mock(LearningContentReader.class);
    private final AiDraftRepository drafts = mock(AiDraftRepository.class);
    private final StartAiDraftUseCase start = mock(StartAiDraftUseCase.class);
    private final SectionQuestionRules rules = new SectionQuestionRules(80, 20_000, 10, .8, .5, 4, 12, 400, 20);
    private final RequestSectionQuestionDraftUseCase useCase;

    RequestSectionQuestionDraftUseCaseTest() {
        version.addSection(section);
        when(packages.findByIdForUpdate(pkg.getId())).thenReturn(Optional.of(pkg));
        when(drafts.findOpenSectionDraft(any(), any())).thenReturn(Optional.empty());
        when(content.packageVersionContent(version.getId())).thenReturn(Optional.of(new PackageVersionContentResult(
                version.getId(), pkg.getId(), pkg.getPackageType(), null, "{}", List.of(new PackageVersionContentResult.Section(
                section.getId(), section.getTitle(), section.getSkill(), section.getInstructions(), 1, 1, null,
                "One passage paragraph with enough wording.", null, List.of(), List.of())))));
        useCase = new RequestSectionQuestionDraftUseCase(packages, content, drafts, start, json, rules);
    }

    @Test void storesOnlyTheSkillSpecificSourceSnapshotAndItsHash() throws Exception {
        UUID author = UUID.randomUUID();
        AiDraft queued = AiDraft.request(DraftKind.SECTION_QUESTIONS, author, "{}", Instant.now());
        when(start.execute(eq(author), eq(DraftKind.SECTION_QUESTIONS), anyString(), anyInt())).thenReturn(queued);
        String passage = "This is a sufficiently long passage with more than eighty words. ".repeat(12);
        when(content.packageVersionContent(version.getId())).thenReturn(Optional.of(new PackageVersionContentResult(
                version.getId(), pkg.getId(), pkg.getPackageType(), null, "{}", List.of(new PackageVersionContentResult.Section(
                section.getId(), section.getTitle(), Skill.READING, section.getInstructions(), 1, 1, null,
                passage, new PackageVersionContentResult.SectionAudio(UUID.randomUUID(), "key", null, 1,
                        "This transcript must not be selected."), List.of(), List.of())))));

        useCase.execute(author, new SectionQuestionDraftRequest(pkg.getId(), version.getId(), section.getId(), QuestionType.FILL_IN_BLANK, 4, null));

        var request = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(start).execute(eq(author), eq(DraftKind.SECTION_QUESTIONS), request.capture(), eq(20));
        JsonNode stored = json.readTree(request.getValue());
        assertEquals("PASSAGE", stored.path("source").path("type").asText());
        assertEquals(passage.strip(), stored.path("source").path("text").asText());
        assertEquals(64, stored.path("source").path("sha256").asText().length());
        assertFalse(stored.path("source").path("text").asText().contains("transcript"));
    }

    @Test void refusesMissingAndTooShortSourcesWithoutConsumingQuota() {
        UUID author = UUID.randomUUID();
        when(content.packageVersionContent(version.getId())).thenReturn(Optional.of(sectionContent(null)));
        var missing = assertThrows(SectionDraftException.class, () -> useCase.execute(author, request()));
        assertTrue(missing.getMessage().contains("nội dung nguồn"));
        when(content.packageVersionContent(version.getId())).thenReturn(Optional.of(sectionContent("short source only")));
        var shortSource = assertThrows(SectionDraftException.class, () -> useCase.execute(author, request()));
        assertTrue(shortSource.getMessage().contains("ít nhất 80 từ"));
        verifyNoInteractions(start);
    }

    @Test void refusesAnOversizedSourceWithoutTruncatingOrConsumingQuota() {
        UUID author = UUID.randomUUID();
        String oversized = "source ".repeat(3_000);
        when(content.packageVersionContent(version.getId())).thenReturn(Optional.of(sectionContent(oversized)));

        SectionDraftException failure = assertThrows(SectionDraftException.class,
                () -> useCase.execute(author, request()));

        assertEquals("SECTION_SOURCE_TOO_LONG", failure.code());
        verifyNoInteractions(start);
    }

    @Test void reusesAnOwnedOpenDraftInsteadOfTakingAnotherUse() {
        AiDraft open = AiDraft.request(DraftKind.SECTION_QUESTIONS, UUID.randomUUID(), "{}", Instant.now());
        when(drafts.findOpenSectionDraft(any(), eq(section.getId()))).thenReturn(Optional.of(open));
        assertThrows(SectionDraftOpenException.class, () -> useCase.execute(UUID.randomUUID(), request()));
        verifyNoInteractions(start);
    }

    private PackageVersionContentResult sectionContent(String passage) {
        return new PackageVersionContentResult(version.getId(), pkg.getId(), pkg.getPackageType(), null, "{}",
                List.of(new PackageVersionContentResult.Section(section.getId(), section.getTitle(), Skill.READING,
                        section.getInstructions(), 1, 1, null, passage, null, List.of(), List.of())));
    }
    private SectionQuestionDraftRequest request() {
        return new SectionQuestionDraftRequest(pkg.getId(), version.getId(), section.getId(), QuestionType.SHORT_ANSWER, 2, null);
    }
}
