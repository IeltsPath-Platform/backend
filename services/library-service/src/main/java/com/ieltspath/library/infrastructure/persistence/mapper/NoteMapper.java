package com.ieltspath.library.infrastructure.persistence.mapper;

import com.ieltspath.library.domain.aggregate.Note;
import com.ieltspath.library.infrastructure.persistence.entity.NoteJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface NoteMapper {
    void copy(Note source, @MappingTarget NoteJpaEntity target);

    Note toDomain(NoteJpaEntity entity);
}
