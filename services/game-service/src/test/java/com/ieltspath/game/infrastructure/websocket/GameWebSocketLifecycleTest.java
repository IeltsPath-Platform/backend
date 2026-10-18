package com.ieltspath.game.infrastructure.websocket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.game.application.result.*;
import com.ieltspath.game.application.usecase.*;
import com.ieltspath.game.domain.exception.GameRoomNotFoundException;
import com.ieltspath.game.domain.repository.GameMatchRepository;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.*;

class GameWebSocketLifecycleTest {
    final UUID room=UUID.randomUUID(),match=UUID.randomUUID(),host=UUID.randomUUID(),guest=UUID.randomUUID();
    final GetGameRoomUseCase rooms=mock(GetGameRoomUseCase.class);
    final SoloGameplayUseCase gameplay=mock(SoloGameplayUseCase.class);
    final GetWordlyRankingUseCase ranking=mock(GetWordlyRankingUseCase.class);
    final GameMatchRepository matches=mock(GameMatchRepository.class);
    final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
    GameWebSocketHandler handler;
    final Map<String,List<String>> received=new HashMap<>();

    @BeforeEach void setup() {
        handler=new GameWebSocketHandler(json,rooms,mock(ChangeGameRoomMembershipUseCase.class),
            mock(StartGameMatchUseCase.class),mock(GetGameMatchStateUseCase.class),mock(SubmitGameAnswerUseCase.class),
            gameplay,ranking,matches);
        when(matches.findActiveIdByRoomId(room)).thenReturn(Optional.empty());
        when(rooms.byId(eq(room),any())).thenReturn(new GameRoomResult(room,"ABC123",host,"WORDLY","VOCABULARY",
            "PRACTICE",4,"WAITING",Instant.now(),List.of(),List.of()));
    }

    WebSocketSession connect(UUID user) throws Exception {
        var session=mock(WebSocketSession.class);
        String id=UUID.randomUUID().toString();
        var open=new AtomicBoolean(true);
        when(session.getId()).thenReturn(id);
        when(session.getAttributes()).thenReturn(Map.of(GameWebSocketHandler.ROOM_ID_ATTRIBUTE,room,
            GameWebSocketHandler.USER_ID_ATTRIBUTE,user));
        when(session.isOpen()).thenAnswer(i->open.get());
        received.put(id,new ArrayList<>());
        doAnswer(i->{received.get(id).add(((TextMessage)i.getArgument(0)).getPayload());return null;})
            .when(session).sendMessage(any());
        doAnswer(i->{open.set(false);return null;}).when(session).close(any());
        handler.afterConnectionEstablished(session);
        return session;
    }

    List<String> types(WebSocketSession session) throws Exception {
        var types=new ArrayList<String>();
        for(var message:received.get(session.getId()))types.add(json.readTree(message).path("type").asText());
        return types;
    }

    @Test void revocationBetweenHandshakeAndRegistrationClosesBeforeSendingRoomState() throws Exception {
        when(rooms.byId(room,guest)).thenThrow(new GameRoomNotFoundException(room));
        var denied=connect(guest);
        assertFalse(denied.isOpen());
        assertTrue(received.get(denied.getId()).isEmpty());
        verify(denied).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test void privateBoardOnlyGoesToItsOwner() throws Exception {
        var a=connect(host);var b=connect(guest);
        received.values().forEach(List::clear);
        handler.soloUpdated(new SoloGameplayEvent(room,host,Map.of("type","WORD_STATE","guess","AIR")));
        assertEquals(List.of("WORD_STATE"),types(a));
        assertTrue(types(b).isEmpty());
    }

    @Test void kickClosesOnlyTargetAndNotifiesInVietnamese() throws Exception {
        var a=connect(host);var b=connect(guest);
        received.values().forEach(List::clear);
        handler.memberKicked(new RoomMemberKickedEvent(room,guest));
        assertTrue(a.isOpen());assertFalse(b.isOpen());
        assertEquals(List.of("PLAYER_KICKED"),types(b));
        assertTrue(received.get(b.getId()).getFirst().contains("chủ phòng"));
    }

    @Test void rankingAndFinalResultArriveBeforeHostClosureAndExcludeNonParticipants() throws Exception {
        var a=connect(host);var b=connect(guest);var outsider=connect(UUID.randomUUID());
        received.values().forEach(List::clear);
        var state=new WordlyRankingResult(match,"COMPLETED","MULTIPLAYER",true,Instant.now(),List.of(
            new WordlyRankingResult.Player(host,"Host",1,2,"FORFEITED",4,100,1000,1,1,1),
            new WordlyRankingResult.Player(guest,"Guest",2,1,"FINISHED",4,200,1000,2,2,2)));
        when(ranking.execute(match,host)).thenReturn(state);
        handler.wordlyUpdated(new WordlyMatchUpdatedEvent(room,match,host,true,"Chủ phòng Host đã đóng phòng"));
        assertEquals(List.of("RANKING_UPDATED","MATCH_FINISHED","ROOM_CLOSED"),types(a));
        assertEquals(List.of("RANKING_UPDATED","MATCH_FINISHED","ROOM_CLOSED"),types(b));
        assertEquals(List.of("ROOM_CLOSED"),types(outsider));
        assertFalse(a.isOpen());assertFalse(b.isOpen());
    }

    @Test void disconnectGraceStartsOnlyAfterUsersLastTabCloses() throws Exception {
        var a=connect(guest);var b=connect(guest);
        when(matches.findActiveIdByRoomId(room)).thenReturn(Optional.of(match));
        a.close(CloseStatus.NORMAL);handler.afterConnectionClosed(a,CloseStatus.NORMAL);
        verify(gameplay,never()).disconnected(any(),any());
        b.close(CloseStatus.NORMAL);handler.afterConnectionClosed(b,CloseStatus.NORMAL);
        verify(gameplay).disconnected(match,guest);
    }

    @Test void restLeaveEventNamesThePlayerWithoutClosingOtherConnections() throws Exception {
        var a=connect(host);var b=connect(guest);
        received.values().forEach(List::clear);
        when(ranking.execute(match,host)).thenReturn(new WordlyRankingResult(match,"IN_PROGRESS","MULTIPLAYER",false,Instant.now(),List.of(
            new WordlyRankingResult.Player(host,"Host",1,1,"FORFEITED",4,0,0,0,0,0),
            new WordlyRankingResult.Player(guest,"Guest",2,2,"ACTIVE",4,0,0,0,0,0))));
        handler.playerLeft(new WordlyPlayerLeftEvent(room,match,host,"Host"));
        assertEquals(List.of("MEMBER_LEFT"),types(b));
        assertTrue(received.get(b.getId()).getFirst().contains("Host đã rời phòng"));
        assertTrue(a.isOpen());assertTrue(b.isOpen());
    }

    @Test void socketOverflowCannotPreventHealthyPlayersReceivingFinalRankingAndClosure() throws Exception {
        var slow=connect(host);var healthy=connect(guest);
        received.values().forEach(List::clear);
        doThrow(new org.springframework.web.socket.handler.SessionLimitExceededException("Buffer full",CloseStatus.SESSION_NOT_RELIABLE))
            .when(slow).sendMessage(any());
        when(ranking.execute(match,host)).thenReturn(new WordlyRankingResult(match,"COMPLETED","MULTIPLAYER",true,Instant.now(),List.of(
            new WordlyRankingResult.Player(host,"Host",1,2,"FORFEITED",4,0,0,0,0,0),
            new WordlyRankingResult.Player(guest,"Guest",2,1,"FINISHED",4,100,1000,1,1,1))));
        assertDoesNotThrow(()->handler.wordlyUpdated(new WordlyMatchUpdatedEvent(room,match,host,true,"Chủ phòng Host đã đóng phòng")));
        assertEquals(List.of("RANKING_UPDATED","MATCH_FINISHED","ROOM_CLOSED"),types(healthy));
        assertFalse(slow.isOpen());
    }
}
