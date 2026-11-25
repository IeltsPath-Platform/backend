package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.PhoneNumber;
import com.ieltspath.user.domain.vo.UserStatus;
import com.ieltspath.user.domain.vo.RoleName;
import com.ieltspath.user.domain.result.UserPage;
import com.ieltspath.user.infrastructure.persistence.entity.UserJpaEntity;
import com.ieltspath.user.infrastructure.persistence.mapper.UserMapper;
import com.ieltspath.user.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {

    @Mock
    private UserJpaRepository userJpaRepository;

    @Mock
    private UserMapper userMapper;

    private UserRepositoryAdapter repositoryAdapter;

    @BeforeEach
    void setUp() {
        repositoryAdapter = new UserRepositoryAdapter(userJpaRepository, userMapper);
    }

    @Test
    void existsByPhoneNumber_delegatesToExistsByPhoneNumber() {
        when(userJpaRepository.existsByPhoneNumber("0912345678")).thenReturn(true);

        boolean exists = repositoryAdapter.existsByPhoneNumber("0912345678");

        assertThat(exists).isTrue();
        verify(userJpaRepository).existsByPhoneNumber("0912345678");
    }

    @Test
    void existsByPhoneNumberAndIdNot_delegatesToExistsByPhoneNumberAndIdNot() {
        UUID id = UUID.randomUUID();
        when(userJpaRepository.existsByPhoneNumberAndIdNot("0912345678", id)).thenReturn(false);

        boolean exists = repositoryAdapter.existsByPhoneNumberAndIdNot("0912345678", id);

        assertThat(exists).isFalse();
        verify(userJpaRepository).existsByPhoneNumberAndIdNot("0912345678", id);
    }

    @Test
    void findById_delegatesToFindWithRolesById() {
        UUID id = UUID.randomUUID();
        UserJpaEntity entity = UserJpaEntity.builder().id(id).build();
        User domain = User.builder().id(id).email(new Email("test@example.com")).build();

        when(userJpaRepository.findWithRolesById(id)).thenReturn(Optional.of(entity));
        when(userMapper.toDomain(entity)).thenReturn(domain);

        Optional<User> result = repositoryAdapter.findById(id);

        assertThat(result).contains(domain);
        verify(userJpaRepository).findWithRolesById(id);
    }

    @Test
    void findAll_delegatesToFindAll() {
        UserJpaEntity entity = UserJpaEntity.builder().id(UUID.randomUUID()).build();
        User domain = User.builder().id(entity.getId()).email(new Email("test@example.com")).build();

        when(userJpaRepository.findAll()).thenReturn(List.of(entity));
        when(userMapper.toDomain(entity)).thenReturn(domain);

        List<User> results = repositoryAdapter.findAll();

        assertThat(results).containsExactly(domain);
        verify(userJpaRepository).findAll();
    }

    @Test
    void findPageFetchesRolesForThePageAndPreservesIdOrder() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        UserJpaEntity firstEntity = UserJpaEntity.builder().id(firstId).build();
        UserJpaEntity secondEntity = UserJpaEntity.builder().id(secondId).build();
        User first = User.builder().id(firstId).email(new Email("first@example.test")).build();
        User second = User.builder().id(secondId).email(new Email("second@example.test")).build();
        when(userJpaRepository.findPageIds("ADMIN", "ACTIVE", "admin", PageRequest.of(0, 2, Sort.unsorted())))
                .thenReturn(new PageImpl<>(List.of(firstId, secondId), PageRequest.of(0, 2), 3));
        when(userJpaRepository.findAllWithRolesByIdIn(List.of(firstId, secondId)))
                .thenReturn(List.of(secondEntity, firstEntity));
        when(userMapper.toDomain(firstEntity)).thenReturn(first);
        when(userMapper.toDomain(secondEntity)).thenReturn(second);

        UserPage page = repositoryAdapter.findPage(RoleName.ADMIN, UserStatus.ACTIVE, "Admin", 0, 2);

        assertThat(page.content()).containsExactly(first, second);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
        verify(userJpaRepository).findAllWithRolesByIdIn(List.of(firstId, secondId));
    }

    @Test
    void save_delegatesToSaveAndToDomain() {
        User domain = User.builder().fullName("Test").status(UserStatus.ACTIVE).build();
        UserJpaEntity entity = UserJpaEntity.builder().build();

        when(userMapper.toEntity(domain)).thenReturn(entity);
        when(userJpaRepository.save(entity)).thenReturn(entity);
        when(userMapper.toDomain(entity)).thenReturn(domain);

        User saved = repositoryAdapter.save(domain);

        assertThat(saved).isEqualTo(domain);
        verify(userJpaRepository).save(entity);
    }
}

