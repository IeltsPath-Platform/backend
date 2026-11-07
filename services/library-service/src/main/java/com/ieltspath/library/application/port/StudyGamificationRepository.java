package com.ieltspath.library.application.port;

import com.ieltspath.library.application.result.StudyGamificationResult.PurchaseResult;
import com.ieltspath.library.domain.study.*;
import java.time.*;
import java.util.*;

public interface StudyGamificationRepository {
    record Profile(UUID userId,String timezone,int totalXp,int level,StudyHearts hearts,Instant maxHeartsCheckedAt) {}
    record Purchase(UUID requestId,UUID userId,String kind,int amount,String status,PurchaseResult result) {}
    Optional<Profile> profile(UUID user);
    void createProfile(UUID user,int maximum,Instant now,String timezone);
    void saveProfile(Profile profile,Instant now);
    StudyStreakState streak(UUID user);
    void saveStreak(UUID user,StudyStreakState state,Instant now);
    int award(UUID user,String source,UUID unit,int amount,LocalDate date,Instant now);
    Optional<Purchase> purchase(UUID request);
    void createPurchase(UUID request,UUID user,String kind,int amount,Instant now);
    void deletePendingPurchase(UUID request);
    void finishPurchase(UUID request,UUID ledger,long balance,PurchaseResult result,Instant now);
}
