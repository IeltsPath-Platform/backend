package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.repository.QuestRepository;
import com.ieltspath.learning.domain.repository.QuestRepository.NewQuest;
import com.ieltspath.learning.domain.repository.QuestRepository.Quest;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.repository.RewardGrantRepository;
import com.ieltspath.learning.domain.service.QuestRules;
import com.ieltspath.learning.domain.service.QuestRules.Candidate;
import com.ieltspath.learning.domain.service.QuestRules.Params;
import com.ieltspath.learning.domain.service.QuestRules.Spec;
import com.ieltspath.learning.domain.service.QuestRules.Type;
import com.ieltspath.learning.domain.vo.MasteryTier;
import com.ieltspath.learning.domain.vo.PendingReview;
import com.ieltspath.learning.domain.vo.QuestionAttemptEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A learner's three daily quests: made on first use of the day, advanced by the answers a submission writes, and paid
 * once when completed. Callers hold the learner lock and run in the submission transaction, so progress and reward commit
 * with the answers that earned them.
 */
@Component
public class QuestService {
    private static final Logger log = LoggerFactory.getLogger(QuestService.class);
    static final String REWARD_REASON = "DAILY_QUEST";

    private final QuestRepository quests;
    private final MasteryProfileRepository profiles;
    private final ReviewItemRepository reviews;
    private final LearningContentClient content;
    private final GamificationRecorder gamification;
    private final RewardGrantRepository rewards;
    private final LearnerTimezones timezones;
    private final Params params;
    private final Clock clock = Clock.systemUTC();

    public QuestService(QuestRepository quests, MasteryProfileRepository profiles, ReviewItemRepository reviews,
                        LearningContentClient content, GamificationRecorder gamification,
                        RewardGrantRepository rewards, LearnerTimezones timezones,
                        @Value("${learning.gamification.quests.practice-required:5}") int practiceRequired,
                        @Value("${learning.gamification.quests.practice-xp:20}") int practiceXp,
                        @Value("${learning.gamification.quests.practice-points:5}") int practicePoints,
                        @Value("${learning.gamification.quests.weakness-required:3}") int weaknessRequired,
                        @Value("${learning.gamification.quests.weakness-xp:30}") int weaknessXp,
                        @Value("${learning.gamification.quests.plus-required:10}") int plusRequired,
                        @Value("${learning.gamification.quests.plus-xp:30}") int plusXp,
                        @Value("${learning.gamification.quests.streak-xp:10}") int streakXp) {
        this.quests = quests;
        this.profiles = profiles;
        this.reviews = reviews;
        this.content = content;
        this.gamification = gamification;
        this.rewards = rewards;
        this.timezones = timezones;
        this.params = new Params(practiceRequired, practiceXp, practicePoints, weaknessRequired, weaknessXp,
                plusRequired, plusXp, streakXp);
    }

    /** The learner's local date now. */
    public LocalDate today(UUID userId) {
        return LocalDate.now(clock.withZone(timezones.zoneOf(userId)));
    }

    /** Today's quests, made now if the day has none yet. */
    public List<Quest> ensureToday(UUID userId, LocalDate today) {
        List<Quest> found = quests.find(userId, today);
        if (!found.isEmpty()) return found;
        Optional<UUID> target = weaknessTarget(userId);
        List<NewQuest> made = new ArrayList<>();
        for (Spec spec : QuestRules.dailySet(params, target.isPresent())) {
            made.add(new NewQuest(spec.type().name(), spec.type() == Type.WEAKNESS ? target.orElseThrow() : null,
                    spec.required(), spec.xp(), spec.points()));
        }
        quests.insertIfAbsent(userId, today, made);
        return quests.find(userId, today);
    }

    /** Advances today's quests with the new answers of a submission and pays those it completes. */
    public void record(UUID userId, List<QuestionAttemptEvent> events) {
        if (events.isEmpty()) return;
        LocalDate today = today(userId);
        List<Quest> todays = ensureToday(userId, today);
        UUID target = todays.stream().filter(quest -> quest.type().equals(Type.WEAKNESS.name())).findFirst()
                .map(Quest::targetKnowledgePointId).orElse(null);
        int practice = 0;
        int weakness = 0;
        for (QuestionAttemptEvent event : events) {
            if (QuestRules.isPracticeAnswer(event.source(), event.correct(), event.firstSubmission())) practice++;
            if (QuestRules.isWeaknessAnswer(event.source(), event.correct(), event.firstSubmission(),
                    event.knowledgePointIds(), target)) {
                weakness++;
            }
        }
        Instant now = clock.instant();
        // Any answer is activity today, which is all the streak quest asks for.
        for (Quest quest : todays) {
            int amount = switch (Type.valueOf(quest.type())) {
                case PRACTICE, PRACTICE_PLUS -> practice;
                case WEAKNESS -> weakness;
                case STREAK -> 1;
            };
            quests.addProgress(userId, today, quest.type(), amount, now).ifPresent(done -> pay(userId, done));
        }
    }

    private void pay(UUID userId, Quest quest) {
        gamification.recordQuestReward(userId, quest.id().toString(), quest.xpReward());
        if (quest.pointsReward() > 0) {
            rewards.enqueue(userId, REWARD_REASON, quest.id().toString(), quest.pointsReward());
        }
    }

    /**
     * The point to work on: a point still being built that has a practice set or a blocking review. Choosing needs the
     * content service; if it cannot answer, the learner gets the longer practice quest instead of an error.
     */
    private Optional<UUID> weaknessTarget(UUID userId) {
        try {
            Set<UUID> blocking = new HashSet<>();
            for (PendingReview review : reviews.findPending(userId)) {
                if (review.blocking()) blocking.add(review.knowledgePointId());
            }
            Set<UUID> withPractice = new HashSet<>();
            for (LearningContentClient.Topic topic : content.getTopicSequence()) {
                for (LearningContentClient.KnowledgePoint point : topic.knowledgePoints()) {
                    if (point.hasPracticeSet()) withPractice.add(point.id());
                }
            }
            List<Candidate> candidates = new ArrayList<>();
            for (MasteryProfile profile : profiles.findAll(userId)) {
                if (profile.tier() != MasteryTier.PRIORITY && profile.tier() != MasteryTier.CONSOLIDATE) continue;
                UUID point = profile.knowledgePointId();
                candidates.add(new Candidate(point, profile.mastery(), blocking.contains(point),
                        blocking.contains(point) || withPractice.contains(point)));
            }
            return QuestRules.pickWeakness(candidates);
        } catch (RuntimeException failure) {
            log.warn("Quest target unavailable, using the longer practice quest: userId={}", userId, failure);
            return Optional.empty();
        }
    }
}
