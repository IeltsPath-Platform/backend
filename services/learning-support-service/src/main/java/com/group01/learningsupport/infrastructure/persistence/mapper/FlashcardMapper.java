package com.group01.learningsupport.infrastructure.persistence.mapper;

import com.group01.learningsupport.domain.aggregate.Flashcard;
import com.group01.learningsupport.infrastructure.persistence.entity.FlashcardJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FlashcardMapper {
    void copy(Flashcard source, @MappingTarget FlashcardJpaEntity target);

    Flashcard toDomain(FlashcardJpaEntity entity);
}
