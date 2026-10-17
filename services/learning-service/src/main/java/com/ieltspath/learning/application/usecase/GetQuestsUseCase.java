package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.service.KnowledgePointDirectory;
import com.ieltspath.learning.application.service.QuestService;
import com.ieltspath.learning.domain.repository.QuestRepository.Quest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Today's quests of the learner, made on first read of the day. */
@Service
public class GetQuestsUseCase {
    public record Target(UUID knowledgePointId, String code, String name) {}

    public record Item(String type, int required, int progress, boolean completed, int xpReward, int pointsReward,
                       Target target) {}

    public record QuestsView(LocalDate date, int completed, List<Item> quests) {}

    private final LearnerLock lock;
    private final QuestService quests;
    private final KnowledgePointDirectory directory;

    public GetQuestsUseCase(LearnerLock lock, QuestService quests, KnowledgePointDirectory directory) {
        this.lock = lock;
        this.quests = quests;
        this.directory = directory;
    }

    @Transactional
    public QuestsView view(UUID userId) {
        lock.lock(userId);
        LocalDate today = quests.today(userId);
        List<Quest> todays = quests.ensureToday(userId, today);
        boolean hasTarget = todays.stream().anyMatch(quest -> quest.targetKnowledgePointId() != null);
        // The directory is one call to Content, asked only when a quest names a point.
        Map<UUID, KnowledgePointDirectory.Entry> described = hasTarget ? directory.describe() : Map.of();
        List<Item> items = todays.stream().map(quest -> new Item(quest.type(), quest.required(), quest.progress(),
                quest.completed(), quest.xpReward(), quest.pointsReward(), target(quest, described))).toList();
        return new QuestsView(today, (int) items.stream().filter(Item::completed).count(), items);
    }

    private static Target target(Quest quest, Map<UUID, KnowledgePointDirectory.Entry> described) {
        if (quest.targetKnowledgePointId() == null) return null;
        KnowledgePointDirectory.Entry meta = described.getOrDefault(quest.targetKnowledgePointId(),
                KnowledgePointDirectory.Entry.UNKNOWN);
        return new Target(quest.targetKnowledgePointId(), meta.code(), meta.name());
    }
}
