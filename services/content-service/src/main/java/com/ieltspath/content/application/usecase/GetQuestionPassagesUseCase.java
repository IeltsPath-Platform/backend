package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.QuestionPassageReader;
import com.ieltspath.content.application.port.QuestionPassageReader.Passage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** The reading passages that go with a question version, so an author can see the question in its context. */
@Service
@Transactional(readOnly = true)
public class GetQuestionPassagesUseCase {

    private final QuestionPassageReader reader;

    public GetQuestionPassagesUseCase(QuestionPassageReader reader) {
        this.reader = reader;
    }

    public List<Passage> execute(UUID questionVersionId) {
        return reader.passagesOf(questionVersionId);
    }
}
