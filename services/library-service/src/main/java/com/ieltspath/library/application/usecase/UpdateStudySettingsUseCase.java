package com.ieltspath.library.application.usecase;
import com.ieltspath.library.application.service.*;
import com.ieltspath.library.application.result.StudyGamificationResult.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class UpdateStudySettingsUseCase {
    private final StudyGamificationService gamification;
    public Settings execute(UUID userId,String timezone){return gamification.settings(userId,timezone);}
}
