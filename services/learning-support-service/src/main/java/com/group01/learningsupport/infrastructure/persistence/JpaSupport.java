package com.group01.learningsupport.infrastructure.persistence;

import com.group01.learningsupport.domain.vo.OwnedPage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.function.Function;

public final class JpaSupport {
    private JpaSupport() {
    }

    public static <T, I> T flush(JpaRepository<T, I> repository, T entity) {
        try {
            return repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            throw PersistenceExceptions.translate(exception);
        }
    }

    public static <E, D> OwnedPage<D> page(Page<E> page, Function<E, D> mapper) {
        return new OwnedPage<>(page.getContent().stream().map(mapper).toList(), page.getTotalElements());
    }
}
