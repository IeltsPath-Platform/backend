package com.group01.learningsupport.infrastructure.persistence.mapper;

import com.group01.learningsupport.domain.aggregate.Note;
import com.group01.learningsupport.infrastructure.persistence.entity.NoteJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface NoteMapper {
    void copy(Note source, @MappingTarget NoteJpaEntity target);

    Note toDomain(NoteJpaEntity entity);
}
