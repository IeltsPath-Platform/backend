package com.group01.game.application.usecase;

import com.group01.game.application.command.CreateGameRoomCommand;
import com.group01.game.application.port.GameContentProvider;
import com.group01.game.application.port.OutboxWriter;
import com.group01.game.application.result.GameRoomResult;
import com.group01.game.domain.aggregate.GameRoom;
import com.group01.game.domain.aggregate.GameType;
import com.group01.game.domain.repository.GameRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CreateGameRoomUseCase {
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final SecureRandom random = new SecureRandom();
    private final GameRoomRepository roomRepository;
    private final GameContentProvider contentProvider;
    private final OutboxWriter outboxWriter;

    public CreateGameRoomUseCase(GameRoomRepository roomRepository, GameContentProvider contentProvider,
                                 OutboxWriter outboxWriter) {
        this.roomRepository = roomRepository;
        this.contentProvider = contentProvider;
        this.outboxWriter = outboxWriter;
    }

    @Transactional
    public GameRoomResult execute(CreateGameRoomCommand command) {
        GameType.require(command.gameType(), command.learningDomain());
        if (command.maxPlayers() < 2 || command.maxPlayers() > 32) {
            throw new IllegalArgumentException("maxPlayers must be between 2 and 32");
        }
        if (command.contentIds().isEmpty() || command.contentIds().size() > 20) {
            throw new IllegalArgumentException("A room requires between 1 and 20 content IDs");
        }
        List<GameContentProvider.GameContentItem> content = contentProvider.loadSnapshot(
                command.gameType(), command.learningDomain(), command.contentIds());
        if (content.size() != command.contentIds().size()) {
            throw new IllegalArgumentException("Some selected content is unavailable");
        }
        List<Map<String, Object>> items = new ArrayList<>(content.size());
        for (var candidate : content) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("canonicalId", candidate.canonicalId());
            item.put("vocabularySenseId", candidate.vocabularySenseId());
            item.put("questionVersionId", candidate.questionVersionId());
            item.put("prompt", candidate.prompt());
            item.put("options", candidate.options());
            item.put("answerSpecJson", candidate.answerSpecJson());
            item.put("explanation", candidate.explanation());
            items.add(item);
        }
        Instant now = Instant.now();
        UUID roomId = UUID.randomUUID();
        String roomCode;
        do { roomCode = generateCode(); } while (roomRepository.findByCode(roomCode).isPresent());
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("contentIds", command.contentIds());
        snapshot.put("items", items);
        snapshot.put("scoring", Map.of("rule", "one-point-per-correct-answer", "version", 1));
        GameRoom room = new GameRoom(roomId, roomCode, command.hostUserId(), command.maxPlayers(),
                command.gameType(), command.learningDomain(), command.mode(), snapshot, now, null, UUID.randomUUID());
        GameRoom saved = roomRepository.save(room);
        outboxWriter.append("GameRoom", roomId.toString(), "GameRoomCreated",
                Map.of("roomId", roomId.toString(), "hostUserId", command.hostUserId().toString(), "roomCode", roomCode));
        return GameRoomResult.from(saved);
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(8);
        for (int i = 0; i < 8; i++) code.append(CODE_CHARS[random.nextInt(CODE_CHARS.length)]);
        return code.toString();
    }
}
