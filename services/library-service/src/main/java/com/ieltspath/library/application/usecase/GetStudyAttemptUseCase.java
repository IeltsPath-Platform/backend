package com.ieltspath.library.application.usecase;
import com.ieltspath.library.application.service.StudyAttemptService;
import com.ieltspath.library.application.result.StudyAttemptResult.*;
import com.ieltspath.library.domain.study.StudyAnswer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class GetStudyAttemptUseCase {
    private final StudyAttemptService service;
    public Attempt execute(UUID userId,UUID attemptId) {return service.get(userId,attemptId);}
}
