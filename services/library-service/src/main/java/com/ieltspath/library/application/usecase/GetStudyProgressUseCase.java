package com.ieltspath.library.application.usecase;
import com.ieltspath.library.application.service.StudyProgressService;
import com.ieltspath.library.application.result.StudyAttemptResult.TopicProgress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service @RequiredArgsConstructor
public class GetStudyProgressUseCase {
    private final StudyProgressService service;
    public List<TopicProgress> execute(UUID userId,List<UUID> topicIds) {return service.execute(userId,topicIds);}
}
