package com.group01.library.infrastructure.adapter;

import com.group01.library.domain.aggregate.FlashcardDeck;
import com.group01.library.domain.repository.FlashcardDeckRepository;
import com.group01.library.domain.vo.LibraryStatus;
import com.group01.library.domain.vo.OwnedPage;
import com.group01.library.infrastructure.persistence.JpaSupport;
import com.group01.library.infrastructure.persistence.entity.FlashcardDeckJpaEntity;
import com.group01.library.infrastructure.persistence.mapper.FlashcardDeckMapper;
import com.group01.library.infrastructure.persistence.repository.FlashcardDeckJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FlashcardDeckRepositoryAdapter implements FlashcardDeckRepository {
    private final FlashcardDeckJpaRepository repository;
    private final FlashcardDeckMapper mapper;

    @Override
    public FlashcardDeck save(FlashcardDeck deck) {
        FlashcardDeckJpaEntity entity = repository.findById(deck.getId()).orElseGet(FlashcardDeckJpaEntity::new);
        mapper.copy(deck, entity);
        return mapper.toDomain(JpaSupport.flush(repository, entity));
    }

    @Override
    public Optional<FlashcardDeck> findByIdAndUserId(UUID id, UUID userId) {
        return repository.findByIdAndUserId(id, userId).map(mapper::toDomain);
    }

    @Override
    public OwnedPage<FlashcardDeck> findByUserIdAndStatus(UUID userId, LibraryStatus status, int page, int size) {
        return JpaSupport.page(
                repository.findByUserIdAndStatus(userId, status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))),
                mapper::toDomain
        );
    }
}
