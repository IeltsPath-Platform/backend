package com.group01.user.infrastructure.persistence;

import com.group01.user.domain.aggregate.AccountActionToken;
import com.group01.user.domain.aggregate.LearnerProfile;
import com.group01.user.domain.aggregate.LearningGoal;
import com.group01.user.domain.aggregate.OAuthIdentity;
import com.group01.user.domain.vo.ActionTokenPurpose;
import com.group01.user.domain.vo.GoalStatus;
import com.group01.user.domain.vo.OAuthProvider;
import com.group01.user.domain.vo.ProfileVisibility;
import com.group01.user.infrastructure.persistence.entity.AccountActionTokenJpaEntity;
import com.group01.user.infrastructure.persistence.entity.LearnerProfileJpaEntity;
import com.group01.user.infrastructure.persistence.entity.LearningGoalJpaEntity;
import com.group01.user.infrastructure.persistence.entity.OAuthIdentityJpaEntity;
import com.group01.user.infrastructure.persistence.entity.UserJpaEntity;
import com.group01.user.infrastructure.persistence.mapper.AccountActionTokenMapper;
import com.group01.user.infrastructure.persistence.mapper.AccountActionTokenMapperImpl;
import com.group01.user.infrastructure.persistence.mapper.LearnerProfileMapper;
import com.group01.user.infrastructure.persistence.mapper.LearnerProfileMapperImpl;
import com.group01.user.infrastructure.persistence.mapper.LearningGoalMapper;
import com.group01.user.infrastructure.persistence.mapper.LearningGoalMapperImpl;
import com.group01.user.infrastructure.persistence.mapper.OAuthIdentityMapper;
import com.group01.user.infrastructure.persistence.mapper.OAuthIdentityMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityMappersTest {

    private LearnerProfileMapper learnerProfileMapper;
    private OAuthIdentityMapper oauthIdentityMapper;
    private AccountActionTokenMapper accountActionTokenMapper;
    private LearningGoalMapper learningGoalMapper;

    @BeforeEach
    void setUp() {
        learnerProfileMapper = new LearnerProfileMapperImpl();
        oauthIdentityMapper = new OAuthIdentityMapperImpl();
        accountActionTokenMapper = new AccountActionTokenMapperImpl();
        learningGoalMapper = new LearningGoalMapperImpl();
    }

    @Test
    void learnerProfileMapper_mapsToDomainAndEntity() {
        UUID userId = UUID.randomUUID();
        LearnerProfileJpaEntity entity = LearnerProfileJpaEntity.builder()
                .userId(userId)
                .displayName("Learner A")
                .timezone("Asia/Ho_Chi_Minh")
                .profileVisibility(ProfileVisibility.COMMUNITY)
                .selfReportedBand(BigDecimal.valueOf(6.5))
                .build();

        LearnerProfile domain = learnerProfileMapper.toDomain(entity);
        assertThat(domain.getUserId()).isEqualTo(userId);
        assertThat(domain.getDisplayName()).isEqualTo("Learner A");
        assertThat(domain.getVisibility()).isEqualTo(ProfileVisibility.COMMUNITY);
        assertThat(domain.getSelfReportedBand()).isEqualByComparingTo("6.5");

        LearnerProfileJpaEntity mappedBack = learnerProfileMapper.toEntity(domain);
        assertThat(mappedBack.getUserId()).isEqualTo(userId);
        assertThat(mappedBack.getDisplayName()).isEqualTo("Learner A");
        assertThat(mappedBack.getProfileVisibility()).isEqualTo(ProfileVisibility.COMMUNITY);
    }

    @Test
    void oauthIdentityMapper_mapsToDomainAndEntity() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserJpaEntity user = UserJpaEntity.builder().id(userId).build();

        OAuthIdentityJpaEntity entity = OAuthIdentityJpaEntity.builder()
                .id(id)
                .user(user)
                .provider(OAuthProvider.GOOGLE)
                .providerSubject("google-sub-123")
                .linkedAt(LocalDateTime.now())
                .build();

        OAuthIdentity domain = oauthIdentityMapper.toDomain(entity);
        assertThat(domain.getId()).isEqualTo(id);
        assertThat(domain.getUserId()).isEqualTo(userId);
        assertThat(domain.getProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(domain.getProviderSubject()).isEqualTo("google-sub-123");
    }

    @Test
    void accountActionTokenMapper_mapsToDomainAndEntity() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserJpaEntity user = UserJpaEntity.builder().id(userId).build();

        AccountActionTokenJpaEntity entity = AccountActionTokenJpaEntity.builder()
                .id(id)
                .user(user)
                .purpose(ActionTokenPurpose.PASSWORD_RESET)
                .tokenHash("token-hash-123")
                .expiresAt(LocalDateTime.now().plusHours(2))
                .createdAt(LocalDateTime.now())
                .build();

        AccountActionToken domain = accountActionTokenMapper.toDomain(entity);
        assertThat(domain.getId()).isEqualTo(id);
        assertThat(domain.getUserId()).isEqualTo(userId);
        assertThat(domain.getPurpose()).isEqualTo(ActionTokenPurpose.PASSWORD_RESET);
        assertThat(domain.getTokenHash()).isEqualTo("token-hash-123");
    }

    @Test
    void learningGoalMapper_mapsToDomainAndEntity() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserJpaEntity user = UserJpaEntity.builder().id(userId).build();
        LocalDate examDate = LocalDate.now().plusMonths(3);

        LearningGoalJpaEntity entity = LearningGoalJpaEntity.builder()
                .id(id)
                .user(user)
                .targetBand(BigDecimal.valueOf(7.5))
                .examDate(examDate)
                .availableMinutesPerDay(60)
                .status(GoalStatus.ACTIVE)
                .startedAt(LocalDateTime.now())
                .build();

        LearningGoal domain = learningGoalMapper.toDomain(entity);
        assertThat(domain.getId()).isEqualTo(id);
        assertThat(domain.getUserId()).isEqualTo(userId);
        assertThat(domain.getTargetBand()).isEqualByComparingTo("7.5");
        assertThat(domain.getExamDate()).isEqualTo(examDate);
        assertThat(domain.getAvailableMinutesPerDay()).isEqualTo(60);
        assertThat(domain.getStatus()).isEqualTo(GoalStatus.ACTIVE);
    }
}

