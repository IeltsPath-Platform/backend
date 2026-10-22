package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.QuestionVersionUsageLookup;
import com.ieltspath.content.application.result.QuestionDetailResult;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.exception.QuestionNotFoundException;
import com.ieltspath.content.domain.exception.QuestionVersionInUseException;
import com.ieltspath.content.domain.exception.QuestionVersionNotFoundException;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.exception.QuestionVersionStateException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ArchiveQuestionVersionUseCase {
    private final QuestionRepository questions;
    private final QuestionVersionUsageLookup usageLookup;

    public ArchiveQuestionVersionUseCase(QuestionRepository questions, QuestionVersionUsageLookup usageLookup) {
        this.questions = questions;
        this.usageLookup = usageLookup;
    }

    public QuestionDetailResult execute(UUID questionId, UUID versionId) {
        Question question = questions.findByIdForUpdate(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
        var version = question.getVersions().stream().filter(v -> v.getId().equals(versionId)).findFirst()
                .orElseThrow(() -> new QuestionVersionNotFoundException(versionId));
        if (version.getStatus() != PublicationStatus.PUBLISHED) {
            throw new QuestionVersionStateException("Only published question versions can be archived");
        }
        if (usageLookup.isUsedByPublishedContent(versionId)) throw new QuestionVersionInUseException(versionId);
        question.archiveVersion(versionId);
        return QuestionDetailResult.from(questions.save(question));
    }
}
