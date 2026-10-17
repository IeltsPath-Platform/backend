package com.ieltspath.game.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.ieltspath.game.application.port.WordlyHintRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class WordlyHintClockTest {
    @Test void deadlineUsesInjectedClockIncludingExactBoundary() {
        var hints=mock(WordlyHintRepository.class);
        UUID match=UUID.randomUUID(),word=UUID.randomUUID(),user=UUID.randomUUID(),request=UUID.randomUUID(),source=UUID.randomUUID();
        Instant deadline=Instant.parse("2026-10-17T00:03:00Z");
        when(hints.lock(match,word,user)).thenReturn(Optional.of(new WordlyHintRepository.Context(
            word,"AIR","ACTIVE","ACTIVE","IN_PROGRESS",deadline,null,null,null,null)));
        when(hints.guesses(word)).thenReturn(List.of(new WordlyHintRepository.Guess(source,"EAR",List.of("ABSENT","PRESENT","CORRECT"))));
        var expired=new UseWordlyHintUseCase(hints,Clock.fixed(deadline,ZoneOffset.UTC));
        assertThrows(IllegalStateException.class,()->expired.execute(match,word,user,request));
        Instant before=deadline.minusMillis(1);
        var active=new UseWordlyHintUseCase(hints,Clock.fixed(before,ZoneOffset.UTC));
        var result=active.execute(match,word,user,request);
        assertEquals(0,result.revealedCell().column());
        verify(hints).save(word,request,source,0,"A",before);
    }
}
