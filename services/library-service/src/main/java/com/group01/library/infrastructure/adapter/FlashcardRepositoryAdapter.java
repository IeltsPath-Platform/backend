package com.group01.library.infrastructure.adapter;

import com.group01.library.domain.aggregate.Flashcard;
import com.group01.library.domain.repository.FlashcardRepository;
import com.group01.library.domain.vo.FlashcardSourceType;
import com.group01.library.domain.vo.LibraryStatus;
import com.group01.library.domain.vo.OwnedPage;
import com.group01.library.infrastructure.persistence.JpaSupport;
import com.group01.library.infrastructure.persistence.entity.FlashcardJpaEntity;
import com.group01.library.infrastructure.persistence.mapper.FlashcardMapper;
import com.group01.library.infrastructure.persistence.repository.FlashcardJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FlashcardRepositoryAdapter implements FlashcardRepository {
    private final FlashcardJpaRepository repository;
    private final FlashcardMapper mapper;

    @Override
    public Flashcard save(Flashcard flashcard) {
        FlashcardJpaEntity entity = repository.findById(flashcard.getId()).orElseGet(FlashcardJpaEntity::new);
        mapper.copy(flashcard, entity);
        return mapper.toDomain(JpaSupport.flush(repository, entity));
    }

    @Override
    public Optional<Flashcard> findByIdAndUserId(UUID id, UUID userId) {
        return repository.findByIdAndUserId(id, userId).map(mapper::toDomain);
    }

    @Override
    public OwnedPage<Flashcard> findByUserIdAndStatus(UUID userId, LibraryStatus status, int page, int size) {
        return JpaSupport.page(
                repository.findByUserIdAndStatus(userId, status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))),
                mapper::toDomain
        );
    }

    @Override
    public Optional<Flashcard> findLivePracticeQuestionCard(UUID userId, UUID practiceQuestionId) {
        return repository.findFirstByUserIdAndSourceTypeAndSourceReferenceIdAndStatusNot(
                userId, FlashcardSourceType.PRACTICE_QUESTION, practiceQuestionId, LibraryStatus.DELETED
        ).map(mapper::toDomain);
    }
}
