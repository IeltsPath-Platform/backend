package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.port.OutboxWriter;
import com.ieltspath.game.application.port.WordlyWordProvider;
import com.ieltspath.game.application.result.MatchStartedEvent;
import com.ieltspath.game.application.result.RocketRoundEvents;
import com.ieltspath.game.application.result.GameMatchResult;
import com.ieltspath.game.domain.aggregate.GameMatch;
import com.ieltspath.game.domain.aggregate.GameMatchPlayer;
import com.ieltspath.game.domain.aggregate.GameRoom;
import com.ieltspath.game.domain.aggregate.RocketRound;
import com.ieltspath.game.domain.exception.GameRoomNotFoundException;
import com.ieltspath.game.domain.exception.InvalidGameRoomStateException;
import com.ieltspath.game.domain.repository.GameMatchPlayerRepository;
import com.ieltspath.game.domain.repository.GameMatchRepository;
import com.ieltspath.game.domain.repository.GameRoomRepository;
import com.ieltspath.game.domain.repository.RocketRoundRepository;
import com.ieltspath.game.domain.vo.RocketDistractorPicker;
import com.ieltspath.game.domain.vo.RocketRoomSettings;
import com.ieltspath.game.domain.vo.RocketWord;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Starts a Rocket match from a ready room: picks the questions from the vocabulary catalog, creates every round with
 * its options, and opens the first round. The catalog is read before the room lock, as Wordly does. MATCH_STARTED goes to
 * the room after commit, which moves every player from the lobby into the match.
 */
@Service
public class StartRocketMatchUseCase {
    private final GameRoomRepository rooms;
    private final GameMatchRepository matches;
    private final GameMatchPlayerRepository players;
    private final RocketRoundRepository rounds;
    private final WordlyWordProvider words;
    private final OutboxWriter outbox;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;
    private final Clock clock;

    public StartRocketMatchUseCase(GameRoomRepository rooms, GameMatchRepository matches,
            GameMatchPlayerRepository players, RocketRoundRepository rounds, WordlyWordProvider words,
            OutboxWriter outbox, ApplicationEventPublisher events, PlatformTransactionManager manager, Clock clock) {
        this.rooms = rooms;
        this.matches = matches;
        this.players = players;
        this.rounds = rounds;
        this.words = words;
        this.outbox = outbox;
        this.events = events;
        this.tx = new TransactionTemplate(manager);
        this.clock = clock;
    }

    public GameMatchResult execute(GameRoom preview, UUID hostUserId) {
        if (!(preview.settings() instanceof RocketRoomSettings settings))
            throw new InvalidGameRoomStateException("Room is not a Rocket room");
        UUID matchId = UUID.randomUUID();
        List<RocketRound> prepared = prepareRounds(matchId, settings, vocabulary());
        return tx.execute(status -> {
            GameRoom room = rooms.findForUpdate(preview.id()).orElseThrow(() -> new GameRoomNotFoundException(preview.id()));
            if (!settings.equals(room.settings()))
                throw new InvalidGameRoomStateException("Room settings changed; retry start");
            Instant now = clock.instant();
            room.startMatch(hostUserId, now);
            rooms.save(room);
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("mode", room.mode());
            config.put("playerMode", room.playerMode());
            config.put("initialLives", settings.initialLives());
            config.put("questionCount", settings.questionCount());
            config.put("secondsPerQuestion", settings.secondsPerQuestion());
            GameMatch match = matches.save(new GameMatch(matchId, room.id(), room.gameType(), room.learningDomain(), config,
                    room.configSnapshot(), now));
            for (GameRoom.Member member : room.members().values()) {
                if (!member.active()) continue;
                players.save(new GameMatchPlayer(UUID.randomUUID(), matchId, member.id(), member.userId(), 0, null,
                        GameMatchPlayer.Status.ACTIVE, now, null, settings.initialLives()));
            }
            rounds.createAll(prepared);
            RocketRound first = prepared.getFirst();
            first.open(now, settings.timeLimit());
            rounds.save(first);
            outbox.append("GameMatch", matchId.toString(), "GameMatchStarted",
                    Map.of("matchId", matchId.toString(), "roomId", room.id().toString(), "gameType", room.gameType()));
            events.publishEvent(new MatchStartedEvent(room.id(), matchId));
            // After MATCH_STARTED, so a lobby has moved into the match before the first round arrives.
            events.publishEvent(RocketRoundEvents.Started.of(room.id(), matchId,
                    settings.questionCount(), first, now));
            return new GameMatchResult(match.id(), match.roomId(), null, match.status().name(), match.startedAt(),
                    GameMatchResult.players(players.findByMatchId(matchId)));
        });
    }

    /** Valid, de-duplicated catalog entries as Rocket words; options are shown in lower case. */
    private List<RocketWord> vocabulary() {
        Map<String, RocketWord> unique = new LinkedHashMap<>();
        for (var word : words.catalog()) {
            if (word.sourceWordId() == null || word.answer() == null || word.answer().isBlank()
                    || word.meaning() == null || word.meaning().isBlank()) continue;
            String text = word.answer().trim().toLowerCase(Locale.ROOT);
            unique.putIfAbsent(text, new RocketWord(word.sourceWordId(), text, word.meaning().trim()));
        }
        return List.copyOf(unique.values());
    }

    static List<RocketRound> prepareRounds(UUID matchId, RocketRoomSettings settings, List<RocketWord> vocabulary) {
        if (vocabulary.size() < Math.max(settings.questionCount(), RocketDistractorPicker.DISTRACTORS + 1))
            throw new IllegalStateException("Not enough eligible vocabulary to start this match");
        List<RocketWord> answers = new ArrayList<>(vocabulary);
        Collections.shuffle(answers, new Random(RocketDistractorPicker.seed(matchId, 0)));
        List<RocketRound> prepared = new ArrayList<>(settings.questionCount());
        for (int sequence = 1; sequence <= settings.questionCount(); sequence++) {
            RocketWord answer = answers.get(sequence - 1);
            var pick = RocketDistractorPicker.pick(answer, vocabulary, RocketDistractorPicker.seed(matchId, sequence));
            prepared.add(new RocketRound(UUID.randomUUID(), matchId, sequence, answer.sourceWordId(), answer.meaning(),
                    pick.options(), pick.correctOptionId()));
        }
        return prepared;
    }
}
