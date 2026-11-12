package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.aggregate.WritingSubmission;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.vo.EssayPrompt;
import com.ieltspath.learning.domain.vo.WritingGrade;
import com.ieltspath.learning.domain.vo.WritingSubmissionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** A revision's result says which error kinds were fixed, which came back and which are new. */
class WritingSubmissionViewAssemblerTest {
    private final WritingSubmissionRepository essays = mock(WritingSubmissionRepository.class);
    private final WritingSubmissionViewAssembler assembler = new WritingSubmissionViewAssembler(essays);
    private final UUID userId = UUID.randomUUID();

    private static WritingGrade grade(String... codes) {
        List<WritingGrade.Correction> corrections = java.util.Arrays.stream(codes).map(code ->
                new WritingGrade.Correction("x", "y", "GRAMMAR", code, code, "hint", 1, null, null, null, "ERROR")).toList();
        return new WritingGrade(List.of(), corrections, "s", new BigDecimal("6.0"));
    }

    private WritingSubmission submission(UUID id, UUID previous, WritingGrade grade) {
        WritingSubmission submission = mock(WritingSubmission.class);
        EssayPrompt prompt = new EssayPrompt(UUID.randomUUID(), "Q", "TASK_2", 50, new BigDecimal("6.0"), null, "Sample",
                List.of(), List.of());
        when(submission.id()).thenReturn(id);
        when(submission.userId()).thenReturn(userId);
        when(submission.prompt()).thenReturn(prompt);
        when(submission.status()).thenReturn(WritingSubmissionStatus.GRADED);
        when(submission.grade()).thenReturn(grade);
        when(submission.previousSubmissionId()).thenReturn(previous);
        when(submission.practiceAttemptId()).thenReturn(UUID.randomUUID());
        when(submission.essayText()).thenReturn("x");
        return submission;
    }

    @Test
    void aRevisionCarriesTheDifferenceFromTheVersionBeforeIt() {
        UUID previousId = UUID.randomUUID();
        WritingSubmission earlier = submission(previousId, null, grade("PLURAL", "TENSE"));
        when(essays.findOwned(previousId, userId)).thenReturn(Optional.of(earlier));
        WritingSubmission revision = submission(UUID.randomUUID(), previousId, grade("TENSE", "WORD"));

        var result = assembler.assemble(revision);

        assertEquals(previousId, result.revision().previousSubmissionId());
        assertEquals(List.of("PLURAL"), result.revision().fixedCodes());
        assertEquals(List.of("TENSE"), result.revision().repeatedCodes());
        assertEquals(List.of("WORD"), result.revision().newCodes());
    }

    @Test
    void anOriginalEssayHasNoRevisionPart() {
        assertNull(assembler.assemble(submission(UUID.randomUUID(), null, grade("TENSE"))).revision());
    }

    @Test
    void aRevisionWhoseEarlierVersionCannotBeReadHasNoRevisionPart() {
        UUID previousId = UUID.randomUUID();
        when(essays.findOwned(previousId, userId)).thenReturn(Optional.empty());

        assertNull(assembler.assemble(submission(UUID.randomUUID(), previousId, grade("TENSE"))).revision());
    }
}
