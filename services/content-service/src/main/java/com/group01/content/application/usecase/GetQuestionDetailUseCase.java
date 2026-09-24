package com.group01.content.application.usecase;

import com.group01.content.application.result.QuestionDetailResult;
import com.group01.content.application.result.QuestionVersionResult;
import com.group01.content.domain.aggregate.Question;
import com.group01.content.domain.exception.QuestionNotFoundException;
import com.group01.content.domain.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetQuestionDetailUseCase {

    private final QuestionRepository questionRepository;

    public GetQuestionDetailUseCase(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public QuestionDetailResult execute(UUID id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new QuestionNotFoundException(id));

        List<QuestionVersionResult> versionResults = question.getVersions().stream()
                .map(v -> new QuestionVersionResult(
                        v.getId(),
                        v.getQuestionId(),
                        v.getVersionNumber(),
                        v.getStem(),
                        v.getOptions(),
                        v.getAnswerSpecJson(),
                        v.getExplanation(),
                        v.getDifficulty(),
                        v.getStatus(),
                        v.getCreatedAt(),
                        v.getUpdatedAt(),
                        v.getKnowledgePoints().stream()
                                .map(kp -> new QuestionKnowledgePointResult(
                                        kp.getQuestionVersionId(), kp.getKnowledgePointId(), kp.getWeight()))
                                .toList()
                ))
                .toList();

        return new QuestionDetailResult(
                question.getId(),
                question.getQuestionType(),
                question.getSkill(),
                question.getRequiredFeatureKey(),
                question.getStatus(),
                question.getCurrentPublishedVersionId(),
                question.getCreatedAt(),
                question.getUpdatedAt(),
                versionResults
        );
    }
}
