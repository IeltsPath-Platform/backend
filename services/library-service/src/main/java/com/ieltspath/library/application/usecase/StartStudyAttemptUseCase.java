package com.ieltspath.library.application.usecase;
import com.ieltspath.library.application.service.StudyAttemptService;
import com.ieltspath.library.application.result.StudyAttemptResult.*;
import com.ieltspath.library.domain.study.StudyAnswer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class StartStudyAttemptUseCase {
    private final StudyAttemptService service;
    public Start execute(UUID userId,UUID unitId,UUID requestId,String mode) {return service.start(userId,unitId,requestId,mode);}
}
