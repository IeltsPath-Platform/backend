package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.command.CreateGameRoomCommand;
import com.ieltspath.game.application.command.StartGameSessionCommand;
import com.ieltspath.game.application.port.CurrentPlayerProfile;
import com.ieltspath.game.application.port.GameContentProvider;
import com.ieltspath.game.domain.repository.GameRoomRepository;
import com.ieltspath.game.domain.repository.GameSessionRepository;
import com.ieltspath.game.application.port.WebSocketTicketStore;
import com.ieltspath.game.application.port.WordlyMatchStore;
import com.ieltspath.game.application.port.WordlyRankingRepository;
import com.ieltspath.game.application.result.CurrentWordResult;
import com.ieltspath.game.application.result.RoomClosedEvent;
import com.ieltspath.game.application.result.RoomUpdatedEvent;
import com.ieltspath.game.application.result.WordlyRankingResult;
import com.ieltspath.game.domain.aggregate.GameRoom;
import com.ieltspath.game.domain.aggregate.GameSession;
import com.ieltspath.game.domain.aggregate.GameSessionStatus;
import com.ieltspath.game.domain.exception.GameMatchNotFoundException;
import com.ieltspath.game.domain.exception.GameRoomNotFoundException;
import com.ieltspath.game.domain.exception.GameSessionNotFoundException;
import com.ieltspath.game.domain.exception.InvalidGameRoomStateException;
import com.ieltspath.game.domain.exception.SoloGameplayException;
import com.ieltspath.game.domain.vo.RocketRoomSettings;
import com.ieltspath.game.domain.vo.WordlyRoomSettings;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RoomAndSessionUseCasesTest {
    private final GameRoomRepository rooms = mock(GameRoomRepository.class);
    private final CurrentPlayerProfile profile = mock(CurrentPlayerProfile.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final WordlyRoomAdmission admission = mock(WordlyRoomAdmission.class);
    private final GameContentProvider content = mock(GameContentProvider.class);
    private final GameSessionRepository sessions = mock(GameSessionRepository.class);
    private final ChangeGameRoomMembershipUseCase membership =
            new ChangeGameRoomMembershipUseCase(rooms, profile, events, admission);
    private final UUID host = UUID.randomUUID();
    private final UUID guest = UUID.randomUUID();

    private GameRoom wordlyRoom() {
        var room = GameRoom.wordly(UUID.randomUUID(), "AB7K2Q", host, new WordlyRoomSettings(
                WordlyRoomSettings.PlayerMode.MULTIPLAYER, 4, WordlyRoomSettings.PlayMode.FINITE, 10), Instant.now());
        when(rooms.findById(room.id())).thenReturn(Optional.of(room));
        when(rooms.findForUpdate(room.id())).thenReturn(Optional.of(room));
        when(rooms.save(room)).thenReturn(room);
        return room;
    }

    private static List<GameContentProvider.GameContentItem> items(int count) {
        return IntStream.range(0, count).mapToObj(i -> new GameContentProvider.GameContentItem(
                UUID.randomUUID(), UUID.randomUUID(), null, "prompt " + i, List.of("a", "b"), "{\"answer\":\"a\"}", null)).toList();
    }

    private static List<UUID> ids(int count) {
        return IntStream.range(0, count).mapToObj(i -> UUID.randomUUID()).toList();
    }

    // ---- join, ready, leave, close ----------------------------------------------------------------------------

    @Test
    void joiningARoomGameChecksAdmissionBeforeLockingTheRoom() {
        var room = wordlyRoom();
        when(profile.displayName()).thenReturn("Guest");

        var result = membership.join(room.id(), guest);

        var order = inOrder(admission, rooms);
        order.verify(admission).requireNoActiveGame(guest);
        order.verify(rooms).findForUpdate(room.id());
        assertEquals("Guest", room.members().get(guest).displayName());
        verify(events).publishEvent(new RoomUpdatedEvent(result));
    }

    @Test
    void aPlayerWithAnActiveGameCannotJoinAndTheRoomIsNotLocked() {
        var room = wordlyRoom();
        doThrow(new SoloGameplayException(409, "ACTIVE_GAME_EXISTS", "Active game")).when(admission).requireNoActiveGame(guest);

        var error = assertThrows(SoloGameplayException.class, () -> membership.join(room.id(), guest));

        assertEquals("ACTIVE_GAME_EXISTS", error.code());
        verify(rooms, never()).findForUpdate(any());
        verifyNoInteractions(events);
    }

    @Test
    void joiningByAnUnknownCodeIsNotFound() {
        when(rooms.findByCode("ZZZZZZ")).thenReturn(Optional.empty());
        assertThrows(GameRoomNotFoundException.class, () -> membership.joinByCode("ZZZZZZ", guest));
        verifyNoInteractions(events);
    }

    @Test
    void readyAndNotReadyPersistAndBroadcastTheirOwnStates() {
        var room = wordlyRoom();
        room.join(guest, Instant.now());

        var ready = membership.ready(room.id(), guest, true);
        var notReady = membership.ready(room.id(), guest, false);

        assertEquals("READY", ready.members().stream().filter(member -> member.userId().equals(guest)).findFirst().orElseThrow().status());
        assertEquals("JOINED", notReady.members().stream().filter(member -> member.userId().equals(guest)).findFirst().orElseThrow().status());
        verify(events).publishEvent(new RoomUpdatedEvent(ready));
        verify(events).publishEvent(new RoomUpdatedEvent(notReady));
    }

    @Test
    void readyInARoomThatDoesNotExistIsNotFound() {
        UUID roomId = UUID.randomUUID();
        when(rooms.findForUpdate(roomId)).thenReturn(Optional.empty());
        assertThrows(GameRoomNotFoundException.class, () -> membership.ready(roomId, guest, true));
        verify(rooms, never()).save(any());
    }

    @Test
    void theHostLeavingClosesTheRoomAndTellsEveryoneByName() {
        var room = wordlyRoom();
        room.setMemberDisplayName(host, "Lan");
        room.join(guest, Instant.now());

        var result = membership.leave(room.id(), host);

        assertEquals("CLOSED", result.status());
        verify(events).publishEvent(new RoomClosedEvent(room.id(), "Chủ phòng Lan đã đóng phòng"));
        verify(events, never()).publishEvent(new RoomUpdatedEvent(result));
    }

    @Test
    void onlyTheHostCanCloseTheRoom() {
        var room = wordlyRoom();
        room.join(guest, Instant.now());

        var error = assertThrows(InvalidGameRoomStateException.class, () -> membership.close(room.id(), guest));

        assertEquals("Only the host can perform this action", error.getMessage());
        verify(rooms, never()).save(any());
    }

    @Test
    void theHostClosesAWaitingRoom() {
        var room = wordlyRoom();
        var result = membership.close(room.id(), host);
        assertEquals("CLOSED", result.status());
        verify(rooms).save(room);
        verify(events).publishEvent(any(RoomClosedEvent.class));
    }

    // ---- create a classic room ---------------------------------------------------------------------------------

    private CreateGameRoomUseCase createRoom() {
        return new CreateGameRoomUseCase(rooms, content, profile);
    }

    private static CreateGameRoomCommand roomCommand(UUID host, String type, int maxPlayers, List<UUID> ids) {
        return new CreateGameRoomCommand(host, type, "VOCABULARY", "PRACTICE", maxPlayers, ids);
    }

    @Test
    void aClassicRoomWithTwoPlacesIsCreatedWithAnEightCharacterCode() {
        var contentIds = ids(1);
        when(content.loadSnapshot("WORD_MEANING_MATCH", "VOCABULARY", contentIds)).thenReturn(items(1));
        when(rooms.findByCode(anyString())).thenReturn(Optional.empty());
        when(rooms.save(any())).thenAnswer(call -> call.getArgument(0));
        when(profile.displayName()).thenReturn("Host");

        var result = createRoom().execute(roomCommand(host, "WORD_MEANING_MATCH", 2, contentIds));

        assertEquals(2, result.maxPlayers());
        assertEquals(8, result.roomCode().length());
        verify(rooms).save(any());
    }

    @Test
    void aClassicRoomForOnePlayerIsRejected() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> createRoom().execute(roomCommand(host, "WORD_MEANING_MATCH", 1, ids(1))));
        assertEquals("maxPlayers must be between 2 and 32", error.getMessage());
        verifyNoInteractions(content, rooms);
    }

    @Test
    void aClassicRoomNeedsOneToTwentyContentIds() {
        for (int count : new int[]{0, 21}) {
            var error = assertThrows(IllegalArgumentException.class,
                    () -> createRoom().execute(roomCommand(host, "WORD_MEANING_MATCH", 4, ids(count))));
            assertEquals("A room requires between 1 and 20 content IDs", error.getMessage());
        }
        verifyNoInteractions(content, rooms);
    }

    @Test
    void aClassicRoomWithUnavailableContentIsRejected() {
        var contentIds = ids(2);
        when(content.loadSnapshot("WORD_MEANING_MATCH", "VOCABULARY", contentIds)).thenReturn(items(1));
        var error = assertThrows(IllegalArgumentException.class,
                () -> createRoom().execute(roomCommand(host, "WORD_MEANING_MATCH", 4, contentIds)));
        assertEquals("Some selected content is unavailable", error.getMessage());
        verify(rooms, never()).save(any());
    }

    @Test
    void aClassicRoomWithAnUnknownGameTypeIsRejected() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> createRoom().execute(roomCommand(host, "CHESS", 4, ids(1))));
        assertEquals("Unsupported gameType", error.getMessage());
        verifyNoInteractions(content, rooms);
    }

    // ---- practice sessions -------------------------------------------------------------------------------------

    private StartGameSessionUseCase startSession() {
        return new StartGameSessionUseCase(content, sessions);
    }

    private static StartGameSessionCommand sessionCommand(UUID user, String type, String domain, List<UUID> ids) {
        return new StartGameSessionCommand(user, type, domain, "PRACTICE", null, ids);
    }

    @Test
    void aPracticeSessionWithTwentyItemsStartsInProgress() {
        UUID user = UUID.randomUUID();
        var contentIds = ids(20);
        when(content.loadSnapshot("WORD_MEANING_MATCH", "VOCABULARY", contentIds)).thenReturn(items(20));
        when(sessions.save(any())).thenAnswer(call -> call.getArgument(0));

        var result = startSession().execute(sessionCommand(user, "WORD_MEANING_MATCH", "VOCABULARY", contentIds));

        assertEquals("IN_PROGRESS", result.status());
        verify(sessions).save(any());
    }

    @Test
    void aPracticeSessionNeedsOneToTwentyDistinctContentIds() {
        UUID user = UUID.randomUUID();
        for (int count : new int[]{0, 21}) {
            var error = assertThrows(IllegalArgumentException.class,
                    () -> startSession().execute(sessionCommand(user, "WORD_MEANING_MATCH", "VOCABULARY", ids(count))));
            assertEquals("A session requires between 1 and 20 content IDs", error.getMessage());
        }
        UUID same = UUID.randomUUID();
        var duplicate = assertThrows(IllegalArgumentException.class,
                () -> startSession().execute(sessionCommand(user, "WORD_MEANING_MATCH", "VOCABULARY", List.of(same, same))));
        assertEquals("contentIds must not contain duplicates", duplicate.getMessage());
        verifyNoInteractions(content, sessions);
    }

    @Test
    void aPracticeSessionWithUnpublishedContentIsRejected() {
        var contentIds = ids(3);
        when(content.loadSnapshot("WORD_MEANING_MATCH", "VOCABULARY", contentIds)).thenReturn(items(2));
        var error = assertThrows(IllegalArgumentException.class,
                () -> startSession().execute(sessionCommand(UUID.randomUUID(), "WORD_MEANING_MATCH", "VOCABULARY", contentIds)));
        assertEquals("Some selected content is unpublished, inactive, or unavailable", error.getMessage());
        verify(sessions, never()).save(any());
    }

    @Test
    void aGrammarGameCannotBeStartedForVocabulary() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> startSession().execute(sessionCommand(UUID.randomUUID(), "SENTENCE_COMPLETION", "VOCABULARY", ids(1))));
        assertEquals("gameType is unsupported for learningDomain", error.getMessage());
        verifyNoInteractions(content, sessions);
    }

    @Test
    void anOwnedSessionCanBeAbandonedButNotSomeoneElses() {
        UUID user = UUID.randomUUID();
        var session = new GameSession(UUID.randomUUID(), user, null, null, "WORD_MEANING_MATCH", "VOCABULARY", "PRACTICE",
                Instant.now(), Map.of("items", List.of(Map.of("id", "one"))), "PENDING");
        when(sessions.findOwned(session.id(), user)).thenReturn(Optional.of(session));
        when(sessions.save(session)).thenReturn(session);

        var result = new AbandonGameSessionUseCase(sessions).execute(session.id(), user);

        assertEquals("ABANDONED", result.status());
        assertEquals(GameSessionStatus.ABANDONED, session.status());
        verify(sessions).save(session);

        UUID other = UUID.randomUUID();
        when(sessions.findOwned(session.id(), other)).thenReturn(Optional.empty());
        assertThrows(GameSessionNotFoundException.class, () -> new AbandonGameSessionUseCase(sessions).execute(session.id(), other));
        assertThrows(GameSessionNotFoundException.class, () -> new GetGameSessionUseCase(sessions).execute(session.id(), other));
    }

    @Test
    void historyPageAndSizeAreChecked() {
        UUID user = UUID.randomUUID();
        var history = new GetGameHistoryUseCase(sessions);
        var negativePage = assertThrows(IllegalArgumentException.class, () -> history.execute(user, -1, 10));
        assertEquals("page must not be negative", negativePage.getMessage());
        for (int size : new int[]{0, 51}) {
            var error = assertThrows(IllegalArgumentException.class, () -> history.execute(user, 0, size));
            assertEquals("size must be between 1 and 50", error.getMessage());
        }
        verifyNoInteractions(sessions);
        when(sessions.findHistory(user, 0, 50)).thenReturn(new GameSessionRepository.SessionPage(List.of(), 0, 0, 50));
        assertEquals(50, history.execute(user, 0, 50).size());
    }

    // ---- settings, current word, ticket -------------------------------------------------------------------------

    @Test
    void settingsGoToTheRightGameAndAreBroadcast() {
        var wordly = wordlyRoom();
        var update = new UpdateRoomSettingsUseCase(rooms, events);
        var wordlyResult = update.execute(wordly.id(), host, 3, 5, true);
        assertEquals(3, wordly.wordlySettings().initialLives());
        verify(events).publishEvent(new RoomUpdatedEvent(wordlyResult));

        var rocket = GameRoom.open(UUID.randomUUID(), "RK7K2Q", host, RocketRoomSettings.multiplayer(10, 4, 15), Instant.now());
        when(rooms.findForUpdate(rocket.id())).thenReturn(Optional.of(rocket));
        when(rooms.save(rocket)).thenReturn(rocket);
        update.execute(rocket.id(), host, 5, null, null, 12, 20);
        var settings = (RocketRoomSettings) rocket.settings();
        assertEquals(5, settings.initialLives());
        assertEquals(12, settings.questionCount());
        assertEquals(20, settings.secondsPerQuestion());
    }

    @Test
    void settingsOfARoomThatDoesNotExistAreNotFound() {
        UUID roomId = UUID.randomUUID();
        when(rooms.findForUpdate(roomId)).thenReturn(Optional.empty());
        assertThrows(GameRoomNotFoundException.class, () -> new UpdateRoomSettingsUseCase(rooms, events).execute(roomId, host, 3, 5, true));
        verifyNoInteractions(events);
    }

    @Test
    void theCurrentWordExplainsWhyThereIsNoneAnyMore() {
        var store = mock(WordlyMatchStore.class);
        var rankings = mock(WordlyRankingRepository.class);
        var useCase = new GetCurrentWordUseCase(store, rankings);
        UUID match = UUID.randomUUID(), user = UUID.randomUUID();

        var current = mock(CurrentWordResult.class);
        when(store.current(match, user)).thenReturn(Optional.of(current));
        assertSame(current, useCase.execute(match, user));

        UUID other = UUID.randomUUID();
        assertThrows(GameMatchNotFoundException.class, () -> useCase.execute(match, other));

        var player = new WordlyRankingResult.Player(user, "Me", 1, 2, "ELIMINATED", 0, 100, 0, 3, 1, 0);
        UUID solo = UUID.randomUUID(), live = UUID.randomUUID(), done = UUID.randomUUID();
        when(rankings.find(solo, user)).thenReturn(Optional.of(new WordlyRankingResult(solo, "IN_PROGRESS", "SOLO", false, Instant.now(), List.of(player))));
        when(rankings.find(live, user)).thenReturn(Optional.of(new WordlyRankingResult(live, "IN_PROGRESS", "MULTIPLAYER", false, Instant.now(), List.of(player))));
        when(rankings.find(done, user)).thenReturn(Optional.of(new WordlyRankingResult(done, "COMPLETED", "MULTIPLAYER", true, Instant.now(), List.of(player))));

        assertThrows(GameMatchNotFoundException.class, () -> useCase.execute(solo, user));
        var notActive = assertThrows(SoloGameplayException.class, () -> useCase.execute(live, user));
        assertEquals(409, notActive.status());
        assertEquals("PLAYER_NOT_ACTIVE", notActive.code());
        assertEquals("Player status: ELIMINATED", notActive.getMessage());
        assertEquals("MATCH_FINISHED", assertThrows(SoloGameplayException.class, () -> useCase.execute(done, user)).code());
    }

    @Test
    void aWebSocketTicketIsOnlyIssuedToAnActiveMember() {
        var tickets = mock(WebSocketTicketStore.class);
        var room = wordlyRoom();
        Instant expires = Instant.parse("2026-10-30T00:01:00Z");
        when(tickets.issue(room.id(), host)).thenReturn(new WebSocketTicketStore.Ticket("t0k3n", room.id(), host, expires));
        var useCase = new CreateWebSocketTicketUseCase(rooms, tickets);

        var ticket = useCase.execute(room.id(), host);

        assertEquals("t0k3n", ticket.ticket());
        assertEquals(expires, ticket.expiresAt());
        assertEquals(List.of("game.v1", "ticket.t0k3n"), ticket.subprotocols());
        assertThrows(GameRoomNotFoundException.class, () -> useCase.execute(room.id(), guest));
        verify(tickets, never()).issue(room.id(), guest);
    }
}
