package com.ieltspath.library.application.service;

import com.ieltspath.library.application.exception.StudyRequestException;
import com.ieltspath.library.application.port.*;
import com.ieltspath.library.application.port.StudyGamificationRepository.Profile;
import com.ieltspath.library.application.result.StudyGamificationResult.*;
import com.ieltspath.library.config.StudyGamificationProperties;
import com.ieltspath.library.domain.study.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class StudyGamificationService {
    private final StudyGamificationRepository repository;
    private final StudyGamificationProperties properties;
    private final StudyStreakActivity streakActivity;
    private final StudyLock lock;
    private final Clock clock;
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Summary summary(UUID user) {
        Instant now=clock.instant();Profile profile=repository.profile(user).orElseGet(()->defaults(user,now));
        profile.hearts().regen(now);return view(profile,repository.streak(user),now);
    }
    @Transactional public Settings settings(UUID user,String timezone) {
        if(timezone==null || !ZoneId.getAvailableZoneIds().contains(timezone))throw new StudyRequestException(HttpStatus.BAD_REQUEST,"INVALID_TIMEZONE","Múi giờ phải là tên IANA hợp lệ.",Map.of("field","timezone"));
        lock.lock(user);Instant now=clock.instant();var profile=profile(user,now);profile.hearts().regen(now);
        repository.saveProfile(new Profile(user,timezone,profile.totalXp(),profile.level(),profile.hearts(),profile.maxHeartsCheckedAt()),now);return new Settings(timezone);
    }
    public Profile requireHearts(UUID user) {
        Instant now=clock.instant();var p=profile(user,now);p.hearts().regen(now);
        if(p.hearts().hearts()==0)throw new StudyRequestException(HttpStatus.CONFLICT,"VOCAB_NO_HEARTS","Bạn đã hết tim. Chờ hồi tim hoặc dùng điểm để hồi đầy.",Map.of("nextHeartAt",p.hearts().nextHeartAt().orElseThrow().toString(),"refillCost",Integer.toString(properties.getHearts().getRefillCost())));
        repository.saveProfile(p,now);return p;
    }
    public HeartsView afterAnswer(Profile profile,boolean lose) {
        Instant now=clock.instant();if(lose)profile.hearts().lose(1,now);repository.saveProfile(profile,now);return hearts(profile.hearts());
    }
    public Reward completed(UUID user,UUID unit,boolean first,int graded,int firstTry) {
        Instant now=clock.instant();var profile=profile(user,now);profile.hearts().regen(now);
        LocalDate today=LocalDate.ofInstant(now,ZoneId.of(profile.timezone()));int completion=0,bonus=0;
        if(first) {
            completion=repository.award(user,"UNIT_FIRST_COMPLETION",unit,properties.getXp().getFirstCompletion(),today,now);
            if(completion>0 && graded>0 && firstTry==graded)bonus=repository.award(user,"UNIT_PERFECT_BONUS",unit,properties.getXp().getPerfectBonus(),today,now);
        }
        int total=Math.addExact(profile.totalXp(),completion+bonus);int level=StudyXpRules.level(total,properties.getXp().getLevelXp());boolean leveled=level>profile.level();
        if(leveled)profile.hearts().refillFull(now);
        boolean increased=!today.equals(repository.streak(user).lastActive());streakActivity.recordActivity(user,now);
        var updated=new Profile(user,profile.timezone(),total,level,profile.hearts(),profile.maxHeartsCheckedAt());repository.saveProfile(updated,now);
        return new Reward(completion+bonus,new XpBreakdown(completion,bonus),leveled,increased,view(updated,repository.streak(user),now));
    }
    public int checkPurchase(UUID user,String kind) {
        Instant now=clock.instant();var profile=profile(user,now);profile.hearts().regen(now);
        if(kind.equals("HEART_REFILL")) {
            if(profile.hearts().full())throw StudyRequestException.conflict("HEARTS_FULL","Tim đã đầy.");
            return properties.getHearts().getRefillCost();
        }
        var state=StudyStreakRules.forRead(repository.streak(user),LocalDate.ofInstant(now,ZoneId.of(profile.timezone())));
        String blocked=blocked(state,LocalDate.ofInstant(now,ZoneId.of(profile.timezone())));
        if(blocked!=null)throw StudyRequestException.conflict(switch(blocked){case "ALREADY_OWNED"->"FREEZE_ALREADY_OWNED";case "NO_STREAK"->"NO_STREAK_TO_PROTECT";default->"FREEZE_LIMIT";},switch(blocked){case "ALREADY_OWNED"->"Bạn đang có một lượt đóng băng.";case "NO_STREAK"->"Chưa có chuỗi học để bảo vệ.";default->"Đã đạt giới hạn đóng băng liên tiếp.";});
        return StudyStreakRules.freezeCost(state,LocalDate.ofInstant(now,ZoneId.of(profile.timezone())),properties.getStreak().getFreezePrice());
    }
    public PurchaseResult applyPurchase(UUID user,String kind,int amount,long balance) {
        Instant now=clock.instant();var profile=profile(user,now);profile.hearts().regen(now);
        if(kind.equals("HEART_REFILL")) {
            profile.hearts().refillFull(now);repository.saveProfile(profile,now);return new PurchaseResult(hearts(profile.hearts()),null,amount,balance);
        }
        LocalDate today=LocalDate.ofInstant(now,ZoneId.of(profile.timezone()));var state=StudyStreakRules.buyFreeze(repository.streak(user),today);
        repository.saveStreak(user,state,now);return new PurchaseResult(null,streak(state,today,profile.timezone()),amount,balance);
    }
    private Profile profile(UUID user,Instant now) {
        var p=repository.profile(user);if(p.isPresent())return p.get();
        repository.createProfile(user,properties.getHearts().getMax(),now,properties.getDefaultTimezone());return repository.profile(user).orElseThrow();
    }
    private Profile defaults(UUID user,Instant now){return new Profile(user,properties.getDefaultTimezone(),0,1,StudyHearts.start(user,properties.getHearts().getMax(),now,Duration.ofHours(properties.getHearts().getRegenHours())),null);}
    private Summary view(Profile profile,StudyStreakState state,Instant now) {
        int next=StudyXpRules.nextLevelAt(profile.totalXp(),properties.getXp().getLevelXp());LocalDate today=LocalDate.ofInstant(now,ZoneId.of(profile.timezone()));
        return new Summary(new XpView(profile.totalXp(),StudyXpRules.level(profile.totalXp(),properties.getXp().getLevelXp()),next,next-profile.totalXp()),streak(StudyStreakRules.forRead(state,today),today,profile.timezone()),hearts(profile.hearts()));
    }
    public HeartsView hearts(StudyHearts hearts){return new HeartsView(hearts.hearts(),hearts.max(),hearts.nextHeartAt().orElse(null),properties.getHearts().getRefillCost());}
    private StreakView streak(StudyStreakState state,LocalDate today,String timezone) {
        String blocked=blocked(state,today);return new StreakView(state.currentDays(),state.longestDays(),state.lastActive(),today.equals(state.lastActive()),state.freezeAvailable(),blocked==null?StudyStreakRules.freezeCost(state,today,properties.getStreak().getFreezePrice()):null,blocked,state.lostStreakDays(),timezone);
    }
    private String blocked(StudyStreakState state,LocalDate today){return state.freezeAvailable()?"ALREADY_OWNED":state.currentDays()==0?"NO_STREAK":StudyStreakRules.overLimit(state,today,properties.getStreak().getMaxConsecutiveFreezes())?"LIMIT":null;}
}
