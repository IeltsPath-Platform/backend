package com.group01.user.infrastructure.persistence;

import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.vo.Email;
import com.group01.user.domain.vo.PhoneNumber;
import com.group01.user.domain.vo.UserStatus;
import com.group01.user.infrastructure.persistence.entity.UserJpaEntity;
import com.group01.user.infrastructure.persistence.mapper.RoleMapperImpl;
import com.group01.user.infrastructure.persistence.mapper.UserMapper;
import com.group01.user.infrastructure.persistence.mapper.UserMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapperImpl();
        ReflectionTestUtils.setField(userMapper, "roleMapper", new RoleMapperImpl());
    }

    @Test
    void toDomain_mapsUserFieldsCorrectly() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        UserJpaEntity entity = UserJpaEntity.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("hashed-pw")
                .fullName("John Doe")
                .phoneNumber("0912345678")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>())
                .createdAt(now)
                .updatedAt(now)
                .build();

        User domain = userMapper.toDomain(entity);

        assertThat(domain).isNotNull();
        assertThat(domain.getId()).isEqualTo(userId);
        assertThat(domain.getEmail().value()).isEqualTo("test@example.com");
        assertThat(domain.getFullName()).isEqualTo("John Doe");
        assertThat(domain.getPhoneNumber()).isNotNull();
        assertThat(domain.getPhoneNumber().value()).isEqualTo("0912345678");
        assertThat(domain.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void toEntity_mapsDomainToEntityCorrectly() {
        UUID userId = UUID.randomUUID();

        User domain = User.builder()
                .id(userId)
                .email(new Email("learner@example.com"))
                .passwordHash("hashed-pw")
                .fullName("Jane Doe")
                .phoneNumber(new PhoneNumber("0987654321"))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>())
                .build();

        UserJpaEntity entity = userMapper.toEntity(domain);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(userId);
        assertThat(entity.getEmail()).isEqualTo("learner@example.com");
        assertThat(entity.getFullName()).isEqualTo("Jane Doe");
        assertThat(entity.getPhoneNumber()).isEqualTo("0987654321");
    }
}
