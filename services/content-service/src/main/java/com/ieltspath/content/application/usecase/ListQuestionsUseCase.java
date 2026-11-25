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
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListQuestionsUseCase {

    public static final int MAX_PAGE_SIZE = 100;

    private final QuestionRepository questionRepository;

    public ListQuestionsUseCase(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /** One page of the catalog, newest first. {@code page} is zero-based. */
    public Page execute(Skill skill, QuestionPurpose purpose, PublicationStatus status, String code, int page, int size) {
        return execute(skill, purpose, status, code, List.of(), page, size);
    }

    public Page execute(Skill skill, QuestionPurpose purpose, PublicationStatus status, String code,
                        List<UUID> knowledgePointIds, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Trang phải từ 0 trở lên và kích thước trang từ 1 đến " + MAX_PAGE_SIZE);
        }
        String normalizedCode = code == null ? "" : code.trim();
        if (knowledgePointIds != null && knowledgePointIds.size() > 50) throw new IllegalArgumentException("Có thể lọc tối đa 50 điểm kiến thức.");
        QuestionRepository.Page result = knowledgePointIds == null || knowledgePointIds.isEmpty()
                ? questionRepository.findPage(skill, purpose, status, normalizedCode, page, size)
                : questionRepository.findPageByKnowledgePoints(skill, purpose, status, normalizedCode, knowledgePointIds, page, size);
        return new Page(result.totalElements(), page, size, result.items().stream().map(QuestionResult::of).toList());
    }

    public Page execute(Skill skill, QuestionPurpose purpose, PublicationStatus status, int page, int size) {
        return execute(skill, purpose, status, null, page, size);
    }

    /** One page of questions that have no owner outside the optional lesson block being edited. */
    public Page executeOnlyFree(Skill skill, QuestionPurpose purpose, PublicationStatus status, String code,
                                UUID exceptBlockId, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Trang phải từ 0 trở lên và kích thước trang từ 1 đến " + MAX_PAGE_SIZE);
        }
        String normalizedCode = code == null ? "" : code.trim();
        QuestionRepository.Page result = questionRepository.findFreePage(skill, purpose, status, normalizedCode,
                exceptBlockId, page, size);
        return new Page(result.totalElements(), page, size, result.items().stream().map(QuestionResult::of).toList());
    }

    public Page executeOnlyFree(Skill skill, QuestionPurpose purpose, PublicationStatus status, String code,
                                UUID exceptBlockId, List<UUID> knowledgePointIds, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Trang phải từ 0 trở lên và kích thước trang từ 1 đến " + MAX_PAGE_SIZE);
        }
        if (knowledgePointIds != null && knowledgePointIds.size() > 50)
            throw new IllegalArgumentException("Có thể lọc tối đa 50 điểm kiến thức.");
        QuestionRepository.Page result = questionRepository.findFreePageByKnowledgePoints(skill, purpose, status,
                code == null ? "" : code.trim(), exceptBlockId, knowledgePointIds, page, size);
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
