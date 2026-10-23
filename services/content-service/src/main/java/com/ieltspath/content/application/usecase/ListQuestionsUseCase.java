package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListQuestionsUseCase {

    public static final int MAX_PAGE_SIZE = 100;

    private final QuestionRepository questionRepository;

    public ListQuestionsUseCase(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /** One page of the catalog, newest first. {@code page} is zero-based. */
    public Page execute(Skill skill, QuestionPurpose purpose, PublicationStatus status, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("page must be >= 0 and size between 1 and " + MAX_PAGE_SIZE);
        }
        QuestionRepository.Page result = questionRepository.findPage(skill, purpose, status, page, size);
        return new Page(result.totalElements(), page, size, result.items().stream().map(QuestionResult::of).toList());
    }

    /** The question carrying this exact import reference, as a page of at most one. */
    public Page findByImportRef(String importRef) {
        String normalized = Question.normalizeImportRef(importRef);
        List<QuestionResult> items = normalized == null
                ? List.of()
                : questionRepository.findByImportRef(normalized).map(QuestionResult::of).stream().toList();
        return new Page(items.size(), 0, MAX_PAGE_SIZE, items);
    }

    public record Page(long totalElements, int page, int size, List<QuestionResult> items) {
        public int totalPages() {
            return (int) ((totalElements + size - 1) / size);
        }
    }
}
