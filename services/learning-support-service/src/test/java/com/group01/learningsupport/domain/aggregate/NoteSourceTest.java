package com.group01.learningsupport.domain.aggregate;

import com.group01.learningsupport.domain.exception.InvalidDataException;
import com.group01.learningsupport.domain.vo.LibraryStatus;
import com.group01.learningsupport.domain.vo.NoteSourceType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
