package com.ieltspath.game.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.game.api.controller.WordlyRankingController;
import com.ieltspath.game.application.port.WordlyRankingRepository;
import com.ieltspath.game.application.result.WordlyRankingResult;
import com.ieltspath.game.domain.exception.GameMatchNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetWordlyRankingUseCaseTest {
    @Test void controllerUsesAuthenticatedIdentityAndPreservesPublicFinalFlag() throws Exception {
        var repository = mock(WordlyRankingRepository.class);
        var identity = mock(CurrentUserProvider.class);
        UUID match = UUID.randomUUID(), user = UUID.randomUUID();
        var result = new WordlyRankingResult(match, "COMPLETED", "MULTIPLAYER", true,
                Instant.now(), List.of());
        when(identity.requireUserId()).thenReturn(user);
        when(repository.find(match, user)).thenReturn(Optional.of(result));
        var useCase = new GetWordlyRankingUseCase(repository);
        assertSame(result, new WordlyRankingController(identity, useCase).get(match));
        verify(repository).find(match, user);
        var json = new ObjectMapper().findAndRegisterModules().valueToTree(result);
        assertTrue(json.get("isFinal").asBoolean());
        assertFalse(json.has("answer"));
        var transaction = GetWordlyRankingUseCase.class.getMethod("execute", UUID.class, UUID.class)
                .getAnnotation(Transactional.class);
        assertTrue(transaction.readOnly());
        assertEquals(Isolation.REPEATABLE_READ, transaction.isolation());
    }

    @Test void missingOrUnauthorizedMatchUsesNotFoundContract() {
        var repository = mock(WordlyRankingRepository.class);
        UUID match = UUID.randomUUID(), user = UUID.randomUUID();
        when(repository.find(match, user)).thenReturn(Optional.empty());
        assertThrows(GameMatchNotFoundException.class,
                () -> new GetWordlyRankingUseCase(repository).execute(match, user));
    }
}
