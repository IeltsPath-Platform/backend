package com.ieltspath.library.infrastructure.persistence.mapper;

import com.ieltspath.library.domain.aggregate.FlashcardDeck;
import com.ieltspath.library.infrastructure.persistence.entity.FlashcardDeckJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FlashcardDeckMapper {
    void copy(FlashcardDeck source, @MappingTarget FlashcardDeckJpaEntity target);

    FlashcardDeck toDomain(FlashcardDeckJpaEntity entity);
}
