package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListQuestionsUseCase {

    private final QuestionRepository questionRepository;

    public ListQuestionsUseCase(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public List<QuestionResult> execute(Skill skill) {
        return execute(skill, null);
    }

    public List<QuestionResult> execute(Skill skill, QuestionPurpose purpose) {
        List<Question> questions;
        if (purpose != null) {
            questions = questionRepository.findByPurpose(purpose, skill);
        } else if (skill != null) {
            questions = questionRepository.findBySkill(skill);
        } else {
            questions = questionRepository.findAll();
        }

        return questions.stream()
                .map(q -> new QuestionResult(
                        q.getId(),
                        q.getQuestionType(),
                        q.getSkill(),
                        q.getRequiredFeatureKey(),
                        q.getStatus(),
                        q.getCurrentPublishedVersionId(),
                        q.getCreatedAt(),
                        q.getUpdatedAt(),
                        q.getPurpose()
                ))
                .toList();
    }
}
