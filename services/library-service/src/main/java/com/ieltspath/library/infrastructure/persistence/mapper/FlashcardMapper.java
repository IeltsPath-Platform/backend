package com.ieltspath.library.infrastructure.persistence.mapper;

import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.infrastructure.persistence.entity.FlashcardJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FlashcardMapper {
    void copy(Flashcard source, @MappingTarget FlashcardJpaEntity target);

    Flashcard toDomain(FlashcardJpaEntity entity);
}
