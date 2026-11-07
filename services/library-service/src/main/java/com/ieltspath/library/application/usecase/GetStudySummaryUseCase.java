package com.ieltspath.library.application.usecase;
import com.ieltspath.library.application.service.*;
import com.ieltspath.library.application.result.StudyGamificationResult.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class GetStudySummaryUseCase {
    private final StudyPlanRefresher plans;
    private final StudyGamificationService gamification;
    public Summary execute(UUID userId){plans.ensureFresh(userId);return gamification.summary(userId);}
}
