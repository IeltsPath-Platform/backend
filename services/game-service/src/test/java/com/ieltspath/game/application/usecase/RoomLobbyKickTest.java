package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.port.CurrentPlayerProfile;
import com.ieltspath.game.application.port.OutboxWriter;
import com.ieltspath.game.application.result.RoomMemberKickedEvent;
import com.ieltspath.game.application.result.RoomUpdatedEvent;
import com.ieltspath.game.domain.aggregate.GameRoom;
import com.ieltspath.game.domain.exception.GameRoomForbiddenException;
import com.ieltspath.game.domain.repository.GameRoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RoomLobbyKickTest {
    @Test
    void locksAndSavesBeforePublishingRemovalAndUpdatedSnapshot() {
        var repository = mock(GameRoomRepository.class);
        var outbox = mock(OutboxWriter.class);
        var events = mock(ApplicationEventPublisher.class);
        var useCase = new ChangeGameRoomMembershipUseCase(repository, outbox, mock(CurrentPlayerProfile.class), events);
        var host = UUID.randomUUID();
        var guest = UUID.randomUUID();
        var room = new GameRoom(UUID.randomUUID(), "AB7K2Q", host, 4, Instant.EPOCH);
        room.join(guest, Instant.EPOCH);
        when(repository.findForUpdate(room.id())).thenReturn(Optional.of(room));
        when(repository.save(room)).thenReturn(room);
        var result = useCase.kick(room.id(), host, guest);
        assertEquals("LEFT", result.members().stream().filter(m -> m.userId().equals(guest)).findFirst().orElseThrow().status());
        var order = inOrder(repository, events);
        order.verify(repository).findForUpdate(room.id());
        order.verify(repository).save(room);
        order.verify(events).publishEvent(new RoomMemberKickedEvent(room.id(), guest));
        order.verify(events).publishEvent(new RoomUpdatedEvent(result));
    }

    @Test
    void unauthorizedKickDoesNotSaveOrPublish() {
        var repository = mock(GameRoomRepository.class);
        var outbox = mock(OutboxWriter.class);
        var events = mock(ApplicationEventPublisher.class);
        var useCase = new ChangeGameRoomMembershipUseCase(repository, outbox, mock(CurrentPlayerProfile.class), events);
        var room = new GameRoom(UUID.randomUUID(), "AB7K2Q", UUID.randomUUID(), 4, Instant.EPOCH);
        when(repository.findForUpdate(room.id())).thenReturn(Optional.of(room));
        assertThrows(GameRoomForbiddenException.class, () -> useCase.kick(room.id(), UUID.randomUUID(), UUID.randomUUID()));
        verify(repository, never()).save(any());
        verifyNoInteractions(events, outbox);
    }
}
