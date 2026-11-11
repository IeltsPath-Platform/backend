package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.exception.InvalidErrorTypeException;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.ErrorReviewStatus;
import com.ieltspath.content.domain.vo.ErrorCategory;
import com.ieltspath.content.domain.vo.ErrorReviewStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ErrorTypeTest {

    @Test
    void aCodeWithoutSkillsAppliesToNoSkillAndIsNotAFallback() {
        ErrorType errorType = ErrorType.create("PARAPHRASE_TRAP", "Paraphrase trap", ErrorCategory.READING_STRATEGY,
                2, null, "Check the original sentence.");

        assertThat(errorType.getSkills()).isEmpty();
        assertThat(errorType.isFallback()).isFalse();
    }

    @Test
    void skillsAreStoredDistinctAndInAStableOrder() {
        ErrorType errorType = new ErrorType(UUID.randomUUID(), "GRAMMAR_SPEAKING_AND_WRITING", "Grammar", ErrorCategory.GRAMMAR,
                1, null, null, "Fix it.", ContentStatus.ACTIVE, ErrorReviewStatus.APPROVED,
                java.time.Instant.now(), java.time.Instant.now(), java.util.List.of("WRITING", "SPEAKING", "WRITING"), false);

        assertThat(errorType.getSkills()).containsExactly("SPEAKING", "WRITING");
    }

    @Test
    void anUnknownSkillIsRejected() {
        ErrorType errorType = ErrorType.create("TENSE", "Tense", ErrorCategory.GRAMMAR, 1, null, "Fix it.");

        assertThatThrownBy(() -> errorType.updateSkills(java.util.List.of("MATH")))
                .isInstanceOf(InvalidErrorTypeException.class);
    }

    @Test
    void updatingSkillsKeepsTheCode() {
        ErrorType errorType = ErrorType.create("TENSE", "Tense", ErrorCategory.GRAMMAR, 1, null, "Fix it.");

        errorType.updateSkills(java.util.List.of("WRITING"));

        assertThat(errorType.getSkills()).containsExactly("WRITING");
        assertThat(errorType.getCode()).isEqualTo("TENSE");
    }

    @Test
    void createsAnActiveErrorTypeWithItsCode() {
        ErrorType errorType = ErrorType.create("PARAPHRASE_TRAP", "Paraphrase trap", ErrorCategory.READING_STRATEGY,
                2, null, "Check the original sentence.");

        assertThat(errorType.getCode()).isEqualTo("PARAPHRASE_TRAP");
        assertThat(errorType.getStatus()).isEqualTo(ContentStatus.ACTIVE);
    }

    @Test
    void rejectsCodesThatAreNotUpperSnakeCase() {
        assertThatThrownBy(() -> ErrorType.create("paraphrase_trap", "x", ErrorCategory.GRAMMAR, 1, null, "hint"))
                .isInstanceOf(InvalidErrorTypeException.class);
        assertThatThrownBy(() -> ErrorType.create("1BAD", "x", ErrorCategory.GRAMMAR, 1, null, "hint"))
                .isInstanceOf(InvalidErrorTypeException.class);
        assertThatThrownBy(() -> ErrorType.create("A".repeat(61), "x", ErrorCategory.GRAMMAR, 1, null, "hint"))
                .isInstanceOf(InvalidErrorTypeException.class);
    }

    @Test
    void severityIsOneToThree() {
        assertThatThrownBy(() -> ErrorType.create("TENSE", "x", ErrorCategory.GRAMMAR, 0, null, "hint"))
                .isInstanceOf(InvalidErrorTypeException.class);
        assertThatThrownBy(() -> ErrorType.create("TENSE", "x", ErrorCategory.GRAMMAR, 4, null, "hint"))
                .isInstanceOf(InvalidErrorTypeException.class);
    }

    @Test
    void fixHintIsRequired() {
        assertThatThrownBy(() -> ErrorType.create("TENSE", "x", ErrorCategory.GRAMMAR, 1, null, "  "))
                .isInstanceOf(InvalidErrorTypeException.class);
    }

    @Test
    void updateRevisesEverythingButTheCode() {
        ErrorType errorType = ErrorType.create("TENSE", "Tense", ErrorCategory.GRAMMAR, 1, null, "hint");
        UUID knowledgePoint = UUID.randomUUID();

        errorType.update("Verb tense", ErrorCategory.GRAMMAR, 2, knowledgePoint, "New hint", ContentStatus.INACTIVE,
                ErrorReviewStatus.APPROVED);

        assertThat(errorType.getCode()).isEqualTo("TENSE");
        assertThat(errorType.getName()).isEqualTo("Verb tense");
        assertThat(errorType.getSeverity()).isEqualTo(2);
        assertThat(errorType.getKnowledgePointId()).isEqualTo(knowledgePoint);
        assertThat(errorType.getStatus()).isEqualTo(ContentStatus.INACTIVE);
        assertThat(errorType.getReviewStatus()).isEqualTo(ErrorReviewStatus.APPROVED);
    }

    @Test
    void newErrorTypesAreDraftUntilReviewed() {
        assertThat(ErrorType.create("TENSE", "Tense", ErrorCategory.GRAMMAR, 1, null, "hint").getReviewStatus())
                .isEqualTo(ErrorReviewStatus.DRAFT);
    }
}
