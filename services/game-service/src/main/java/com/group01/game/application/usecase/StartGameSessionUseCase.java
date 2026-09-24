package com.group01.game.application.usecase;

import com.group01.game.application.command.StartGameSessionCommand;
import com.group01.game.application.port.GameContentProvider;
import com.group01.game.application.port.GameContentProvider.GameContentItem;
import com.group01.game.application.port.OutboxWriter;
import com.group01.game.application.result.GameSessionResult;
import com.group01.game.domain.aggregate.GameSession;
import com.group01.game.domain.aggregate.GameType;
import com.group01.game.domain.repository.GameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class StartGameSessionUseCase {
    private final GameContentProvider contentProvider;
    private final GameSessionRepository sessionRepository;
    private final OutboxWriter outboxWriter;

    public StartGameSessionUseCase(GameContentProvider contentProvider, GameSessionRepository sessionRepository,
                                   OutboxWriter outboxWriter) {
        this.contentProvider = contentProvider;
        this.sessionRepository = sessionRepository;
        this.outboxWriter = outboxWriter;
    }

    @Transactional
    public GameSessionResult execute(StartGameSessionCommand command) {
        GameType.require(command.gameType(), command.learningDomain());
        if (command.contentIds().isEmpty() || command.contentIds().size() > 20) {
            throw new IllegalArgumentException("A session requires between 1 and 20 content IDs");
        }
        if (command.contentIds().stream().distinct().count() != command.contentIds().size()) {
            throw new IllegalArgumentException("contentIds must not contain duplicates");
        }
        List<GameContentItem> candidates = contentProvider.loadSnapshot(command.gameType(),
                command.learningDomain(), command.contentIds());
        if (candidates.size() != command.contentIds().size()) {
            throw new IllegalArgumentException("Some selected content is unpublished, inactive, or unavailable");
        }

        List<Map<String, Object>> snapshotItems = new ArrayList<>(candidates.size());
        for (GameContentItem candidate : candidates) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("canonicalId", candidate.canonicalId());
            item.put("vocabularySenseId", candidate.vocabularySenseId());
            item.put("questionVersionId", candidate.questionVersionId());
            item.put("prompt", candidate.prompt());
            item.put("options", candidate.options());
            item.put("answerSpecJson", candidate.answerSpecJson());
            item.put("explanation", candidate.explanation());
            snapshotItems.add(item);
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("items", snapshotItems);
        snapshot.put("selection", Map.of("contentIds", command.contentIds()));
        snapshot.put("scoring", Map.of("rule", "one-point-per-correct-answer", "version", 1));

        Instant startedAt = Instant.now();
        GameSession session = new GameSession(UUID.randomUUID(), command.userId(), command.topicId(), null,
                command.gameType(), command.learningDomain(), command.mode(), startedAt, snapshot, "PENDING");
        GameSession saved = sessionRepository.save(session);
        outboxWriter.append("GameSession", saved.id().toString(), "GameSessionStarted",
                Map.of("sessionId", saved.id().toString(), "userId", saved.userId().toString(),
                        "learningDomain", saved.learningDomain(), "gameType", saved.gameType()));
        return GameSessionResult.from(saved);
    }

}
