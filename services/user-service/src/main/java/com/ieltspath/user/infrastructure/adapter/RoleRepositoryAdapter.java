package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.domain.aggregate.Role;
import com.ieltspath.user.domain.repository.RoleRepository;
import com.ieltspath.user.infrastructure.persistence.mapper.RoleMapper;
import com.ieltspath.user.infrastructure.persistence.repository.RoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepository {
    private final RoleJpaRepository roleJpaRepository;
    private final RoleMapper roleMapper;

    @Override
    public Optional<Role> findByName(String name) {
        return roleJpaRepository.findByName(name).map(roleMapper::toDomain);
    }

    @Override
    public List<Role> findByNames(Collection<String> names) {
        return roleJpaRepository.findByNameIn(names).stream().map(roleMapper::toDomain).toList();
    }
}
