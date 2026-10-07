package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.domain.aggregate.Note;
import com.ieltspath.library.domain.vo.NoteSourceType;
import com.ieltspath.library.infrastructure.persistence.entity.NoteJpaEntity;
import com.ieltspath.library.infrastructure.persistence.mapper.NoteMapper;
import com.ieltspath.library.infrastructure.persistence.mapper.NoteMapperImpl;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NoteMapperTest {
    private final NoteMapper mapper = new NoteMapperImpl();

    @Test
    void preservesSourceAcrossPersistenceMapping() {
        UUID sourceId = UUID.randomUUID();
        Note note = Note.create(UUID.randomUUID(), "Title", "Body",
                NoteSourceType.KNOWLEDGE_POINT, sourceId);
        NoteJpaEntity entity = new NoteJpaEntity();
        mapper.copy(note, entity);
        assertEquals(NoteSourceType.KNOWLEDGE_POINT, entity.getSourceType());
        assertEquals(sourceId, entity.getSourceReferenceId());
        Note restored = mapper.toDomain(entity);
        assertEquals(NoteSourceType.KNOWLEDGE_POINT, restored.getSourceType());
        assertEquals(sourceId, restored.getSourceReferenceId());
    }

    @Test
    void mapsLegacyNoteWithoutSource() {
        NoteJpaEntity entity = new NoteJpaEntity();
        mapper.copy(Note.create(UUID.randomUUID(), "Title", "Body"), entity);
        Note restored = mapper.toDomain(entity);
        assertNull(restored.getSourceType());
        assertNull(restored.getSourceReferenceId());
    }
}
