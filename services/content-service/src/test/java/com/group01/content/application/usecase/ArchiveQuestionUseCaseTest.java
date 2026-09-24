package com.group01.content.application.usecase;

import com.group01.content.application.result.QuestionResult;
import com.group01.content.domain.aggregate.Question;
import com.group01.content.domain.exception.QuestionNotFoundException;
import com.group01.content.domain.repository.QuestionRepository;
import com.group01.content.domain.vo.PublicationStatus;
import com.group01.content.domain.vo.QuestionType;
import com.group01.content.domain.vo.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchiveQuestionUseCaseTest {

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private ArchiveQuestionUseCase archiveQuestionUseCase;

    private Question question;
    private UUID questionId;

    @BeforeEach
    void setUp() {
        question = Question.create(QuestionType.MULTIPLE_CHOICE, Skill.READING, null);
        questionId = question.getId();
    }

    @Test
    @DisplayName("Should archive question successfully")
    void shouldArchiveQuestionSuccessfully() {
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        QuestionResult result = archiveQuestionUseCase.execute(questionId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(questionId);
        assertThat(result.status()).isEqualTo(PublicationStatus.ARCHIVED);

        verify(questionRepository).findById(questionId);
        verify(questionRepository).save(question);
    }

    @Test
    @DisplayName("Should throw QuestionNotFoundException when question does not exist")
    void shouldThrowWhenQuestionNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(questionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> archiveQuestionUseCase.execute(nonExistentId))
                .isInstanceOf(QuestionNotFoundException.class);
    }
}
