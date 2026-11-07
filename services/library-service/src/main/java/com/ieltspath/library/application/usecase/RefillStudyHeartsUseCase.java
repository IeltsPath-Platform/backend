package com.ieltspath.library.application.usecase;
import com.ieltspath.library.application.service.*;
import com.ieltspath.library.application.result.StudyGamificationResult.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class RefillStudyHeartsUseCase {
    private final StudyPlanRefresher plans;
    private final StudyPurchaseCoordinator purchases;
    public PurchaseResult execute(UUID userId,UUID requestId){plans.ensureFresh(userId);return purchases.purchase(userId,requestId,"HEART_REFILL");}
}
