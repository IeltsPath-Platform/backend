package com.ieltspath.game.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.ieltspath.game.application.port.*;
import com.ieltspath.game.domain.aggregate.*;
import com.ieltspath.game.domain.repository.*;
import com.ieltspath.game.domain.vo.WordlyRoomSettings;
import com.ieltspath.game.domain.exception.InvalidGameRoomStateException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.*;

class StartWordlyMultiplayerTest {
    final UUID host=UUID.randomUUID(),guest=UUID.randomUUID(),roomId=UUID.randomUUID();
    final Clock clock=Clock.fixed(Instant.parse("2026-10-17T00:00:00Z"),ZoneOffset.UTC);
    final GameRoomRepository rooms=mock(GameRoomRepository.class);
    final GameMatchRepository matches=mock(GameMatchRepository.class);
    final GameMatchPlayerRepository players=mock(GameMatchPlayerRepository.class);
    final GameSessionRepository sessions=mock(GameSessionRepository.class);
    final WordlyWordProvider provider=mock(WordlyWordProvider.class);
    final WordlyMatchStore store=mock(WordlyMatchStore.class);
    com.ieltspath.game.domain.vo.WordFilter filter=com.ieltspath.game.domain.vo.WordFilter.ANY;
    final GameRoom room=readyRoom();
    final List<WordlyWordProvider.Word> words=List.of("AIR","BOOK","ACTOR","PLANET","WATER").stream()
        .map(a->new WordlyWordProvider.Word(UUID.randomUUID(),a,"Meaning "+a)).toList();
    final List<GameMatchPlayer> savedPlayers=new ArrayList<>();
    final StartGameMatchUseCase start;

    StartWordlyMultiplayerTest() {
        when(rooms.findById(roomId)).thenAnswer(i->{
            var copy=readyRoom();
            if(!"WAITING".equals(room.status().name()))copy.startMatch(host,clock.instant());
            return Optional.of(copy);
        });
        when(rooms.findForUpdate(roomId)).thenAnswer(i->{
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return Optional.of(room);
        });
        when(rooms.save(any())).thenAnswer(i->i.getArgument(0));
        when(matches.save(any())).thenAnswer(i->i.getArgument(0));
        when(players.save(any())).thenAnswer(i->{GameMatchPlayer p=i.getArgument(0);savedPlayers.add(p);return p;});
        when(players.findByMatchId(any())).thenReturn(savedPlayers);
        when(provider.catalog()).thenAnswer(i->{
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            verify(rooms,never()).findForUpdate(any());
            return words;
        });
        when(store.prepareDictionary(any())).thenReturn("version");
        var manager=new AbstractPlatformTransactionManager() {
            protected Object doGetTransaction(){return new Object();}
            protected void doBegin(Object tx,org.springframework.transaction.TransactionDefinition d){}
            protected void doCommit(DefaultTransactionStatus s){}
            protected void doRollback(DefaultTransactionStatus s){}
        };
        start=new StartGameMatchUseCase(rooms,matches,players,sessions,mock(OutboxWriter.class),
            mock(ApplicationEventPublisher.class),provider,store,manager,clock);
    }
    private GameRoom readyRoom() {
        var value=GameRoom.wordly(roomId,"ABC123",host,new WordlyRoomSettings(
            WordlyRoomSettings.PlayerMode.MULTIPLAYER,4,WordlyRoomSettings.PlayMode.FINITE,5,true,filter),clock.instant());
        value.join(guest,clock.instant());value.setReady(host,true,clock.instant());value.setReady(guest,true,clock.instant());
        return value;
    }
    private void chooseWords() {
        filter=new com.ieltspath.game.domain.vo.WordFilter("A1",UUID.randomUUID());
        room.updateSettings(host,4,5,true,filter,clock.instant());
        room.setReady(host,true,clock.instant());room.setReady(guest,true,clock.instant());
    }
    @Test void answersFollowTheFilterWhileEveryCatalogWordStaysAValidGuess() {
        chooseWords();
        var zebra=new WordlyWordProvider.Word(UUID.randomUUID(),"ZEBRA","Meaning ZEBRA");
        var full=new ArrayList<>(words);full.add(zebra);
        when(provider.catalog()).thenReturn(full);
        when(provider.catalog(filter)).thenReturn(words);
        var result=start.execute(roomId,host);
        verify(store).initialize(eq(result.matchId()),argThat(w->new HashSet<>(w).equals(new HashSet<>(words))),any(),any(),eq(true));
        verify(store).prepareDictionary(argThat(w->w.contains(zebra)));
    }
    @Test void tooFewWordsForTheFilterReportsHowManyThereAre() {
        chooseWords();
        when(provider.catalog(filter)).thenReturn(words.subList(0,2));
        var error=assertThrows(com.ieltspath.game.domain.exception.NotEnoughWordsException.class,()->start.execute(roomId,host));
        assertEquals(2,error.available());
        assertEquals(409,error.status());
        assertEquals("WAITING",room.status().name());
        verify(store,never()).initialize(any(),any(),any(),any(),anyBoolean());
    }
    @Test void loadsVocabularyOutsideLockAndStartsOnceUsingInjectedClock() {
        var result=start.execute(roomId,host);
        assertEquals(2,result.players().size());assertEquals("IN_MATCH",room.status().name());
        assertEquals(clock.instant(),result.startedAt());
        verify(store).initialize(eq(result.matchId()),argThat(w->w.size()==5 && new HashSet<>(w).equals(new HashSet<>(words))),argThat(ids->ids.size()==2),eq(clock.instant()),eq(true));
        verify(store).attachDictionary(result.matchId(),"version");
        assertThrows(InvalidGameRoomStateException.class,()->start.execute(roomId,host));
        verify(provider,times(1)).catalog();
        verify(provider,never()).random(anyInt());
    }
    @Test void changedSettingsDuringLibraryCallPreventMatchCreation() {
        when(provider.catalog()).thenAnswer(i->{room.updateSettings(host,3,5,true,clock.instant());return words;});
        assertThrows(InvalidGameRoomStateException.class,()->start.execute(roomId,host));
        verify(matches,never()).save(any());
        assertEquals("WAITING",room.status().name());
    }
    @Test void changedReadinessDuringLibraryCallPreventsMatchCreation() {
        when(provider.catalog()).thenAnswer(i->{room.setReady(guest,false,clock.instant());return words;});
        assertThrows(InvalidGameRoomStateException.class,()->start.execute(roomId,host));
        verify(matches,never()).save(any());
    }
    @Test void libraryFailureNeverTakesRoomLock() {
        when(provider.catalog()).thenThrow(new IllegalStateException("Library unavailable"));
        assertThrows(IllegalStateException.class,()->start.execute(roomId,host));
        verify(rooms,never()).findForUpdate(any());
        verify(matches,never()).save(any());
        assertEquals("WAITING",room.status().name());
    }
}
