package com.ieltspath.game.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ieltspath.game.application.port.*;
import com.ieltspath.game.domain.aggregate.*;
import com.ieltspath.game.domain.repository.*;
import com.ieltspath.game.domain.vo.WordlyRoomSettings;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class StartWordlyMultiplayerTest {
    @Test void startSnapshotsSharedDictionaryAndCannotBeRepeated() {
        UUID host=UUID.randomUUID(),guest=UUID.randomUUID(),roomId=UUID.randomUUID();
        var room=GameRoom.wordly(roomId,"ABC123",host,new WordlyRoomSettings(
            WordlyRoomSettings.PlayerMode.MULTIPLAYER,4,WordlyRoomSettings.PlayMode.FINITE,5,true),Instant.now());
        room.join(guest,Instant.now());room.setReady(host,true,Instant.now());room.setReady(guest,true,Instant.now());
        var rooms=mock(GameRoomRepository.class);var matches=mock(GameMatchRepository.class);
        var players=mock(GameMatchPlayerRepository.class);var sessions=mock(GameSessionRepository.class);
        var provider=mock(WordlyWordProvider.class);var store=mock(WordlyMatchStore.class);
        var savedPlayers=new ArrayList<GameMatchPlayer>();
        when(rooms.findForUpdate(roomId)).thenReturn(Optional.of(room));
        when(rooms.save(any())).thenAnswer(i->i.getArgument(0));
        when(matches.save(any())).thenAnswer(i->i.getArgument(0));
        when(players.save(any())).thenAnswer(i->{GameMatchPlayer player=i.getArgument(0);savedPlayers.add(player);return player;});
        when(players.findByMatchId(any())).thenReturn(savedPlayers);
        var words=List.of("AIR","BOOK","ACTOR","PLANET","WATER").stream()
            .map(a->new WordlyWordProvider.Word(UUID.randomUUID(),a,"Meaning "+a)).toList();
        when(provider.random(5)).thenReturn(words);
        when(provider.catalog()).thenReturn(words);
        var start=new StartGameMatchUseCase(rooms,matches,players,sessions,mock(OutboxWriter.class),
            mock(ApplicationEventPublisher.class),provider,store);
        var result=start.execute(roomId,host);
        assertEquals(2,result.players().size());assertEquals("IN_MATCH",room.status().name());
        verify(store).initialize(eq(result.matchId()),eq(words),argThat(ids->ids.size()==2),any(),eq(true));
        verify(store).preparePool(eq(result.matchId()),eq(words));
        assertThrows(com.ieltspath.game.domain.exception.InvalidGameRoomStateException.class,()->start.execute(roomId,host));
        verify(provider,times(1)).random(5);
    }
}
