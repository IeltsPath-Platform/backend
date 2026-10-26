package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.WorkItemSubjectsResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetWorkItemSubjectsUseCase {
    private final LearningContentReader reader;

    public GetWorkItemSubjectsUseCase(LearningContentReader reader) {
        this.reader = reader;
    }

    public WorkItemSubjectsResult execute(Collection<UUID> knowledgePointIds, Collection<UUID> questionVersionIds) {
        return reader.workItemSubjects(knowledgePointIds, questionVersionIds);
    }
}
