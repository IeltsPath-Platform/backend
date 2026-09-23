package com.group01.learningsupport.infrastructure.persistence.mapper;

import com.group01.learningsupport.domain.aggregate.FlashcardDeck;
import com.group01.learningsupport.infrastructure.persistence.entity.FlashcardDeckJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FlashcardDeckMapper {
    void copy(FlashcardDeck source, @MappingTarget FlashcardDeckJpaEntity target);

    FlashcardDeck toDomain(FlashcardDeckJpaEntity entity);
}
