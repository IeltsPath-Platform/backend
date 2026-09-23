package com.group01.access.domain.repository;

import com.group01.access.domain.aggregate.KeyProduct;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KeyProductRepository {

    Optional<KeyProduct> findById(UUID id);

    Optional<KeyProduct> findByCode(String code);

    List<KeyProduct> findAll();

    KeyProduct save(KeyProduct keyProduct);
}
