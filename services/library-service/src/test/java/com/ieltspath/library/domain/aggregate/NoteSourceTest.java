package com.ieltspath.library.domain.aggregate;

import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.vo.LibraryStatus;
import com.ieltspath.library.domain.vo.NoteSourceType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoteSourceTest {
    @Test
    void createsNotesWithAndWithoutSource() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        Note sourced = Note.create(userId, "Lesson", "Keep this", NoteSourceType.TUTOR_SESSION, sessionId);
        assertEquals(NoteSourceType.TUTOR_SESSION, sourced.getSourceType());
        assertEquals(sessionId, sourced.getSourceReferenceId());

        Note plain = Note.create(userId, "Manual", "Text");
        assertNull(plain.getSourceType());
        assertNull(plain.getSourceReferenceId());
    }

    @Test
    void rejectsIncompleteSourcePair() {
        UUID userId = UUID.randomUUID();
        assertThrows(InvalidDataException.class, () ->
                Note.create(userId, "Title", "Body", NoteSourceType.KNOWLEDGE_POINT, null));
        assertThrows(InvalidDataException.class, () ->
                Note.create(userId, "Title", "Body", null, UUID.randomUUID()));
    }

    @Test
    void updatePreservesSource() {
        UUID sourceId = UUID.randomUUID();
        Note note = Note.create(UUID.randomUUID(), "Old", "Old body", NoteSourceType.KNOWLEDGE_POINT, sourceId);
        note.update("New", "New body", LibraryStatus.ARCHIVED);
        assertEquals(NoteSourceType.KNOWLEDGE_POINT, note.getSourceType());
        assertEquals(sourceId, note.getSourceReferenceId());
    }

    @Test
    void updateRejectsNullStatusAndSignalsWhenActiveLinksMustBeRemoved() {
        UUID sourceId = UUID.randomUUID();
        Note note = Note.create(UUID.randomUUID(), "Lesson", "Keep this", NoteSourceType.KNOWLEDGE_POINT, sourceId);

        InvalidDataException missingStatus = assertThrows(InvalidDataException.class,
                () -> note.update("Lesson", "Keep this", null));
        assertEquals("status không hợp lệ", missingStatus.getMessage());
        assertEquals(LibraryStatus.ACTIVE, note.getStatus());

        assertTrue(note.update("Lesson", "Updated", LibraryStatus.ARCHIVED));
        assertEquals(LibraryStatus.ARCHIVED, note.getStatus());
        assertEquals(NoteSourceType.KNOWLEDGE_POINT, note.getSourceType());
        assertEquals(sourceId, note.getSourceReferenceId());
    }
}
