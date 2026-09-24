package com.group01.content.application.usecase;

import com.group01.content.application.result.QuestionResult;
import com.group01.content.domain.aggregate.Question;
import com.group01.content.domain.repository.QuestionRepository;
import com.group01.content.domain.vo.Skill;
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
        List<Question> questions;
        if (skill != null) {
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
                        q.getUpdatedAt()
                ))
                .toList();
    }
}
