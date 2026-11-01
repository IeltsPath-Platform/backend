package com.ieltspath.user.infrastructure.persistence;

import com.ieltspath.user.domain.aggregate.AccountActionToken;
import com.ieltspath.user.domain.aggregate.LearnerProfile;
import com.ieltspath.user.domain.aggregate.OAuthIdentity;
import com.ieltspath.user.domain.vo.ActionTokenPurpose;
import com.ieltspath.user.domain.vo.OAuthProvider;
import com.ieltspath.user.domain.vo.ProfileVisibility;
import com.ieltspath.user.infrastructure.persistence.entity.AccountActionTokenJpaEntity;
import com.ieltspath.user.infrastructure.persistence.entity.LearnerProfileJpaEntity;
import com.ieltspath.user.infrastructure.persistence.entity.OAuthIdentityJpaEntity;
import com.ieltspath.user.infrastructure.persistence.entity.UserJpaEntity;
import com.ieltspath.user.infrastructure.persistence.mapper.AccountActionTokenMapper;
import com.ieltspath.user.infrastructure.persistence.mapper.AccountActionTokenMapperImpl;
import com.ieltspath.user.infrastructure.persistence.mapper.LearnerProfileMapper;
import com.ieltspath.user.infrastructure.persistence.mapper.LearnerProfileMapperImpl;
import com.ieltspath.user.infrastructure.persistence.mapper.OAuthIdentityMapper;
import com.ieltspath.user.infrastructure.persistence.mapper.OAuthIdentityMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityMappersTest {

    private LearnerProfileMapper learnerProfileMapper;
    private OAuthIdentityMapper oauthIdentityMapper;
    private AccountActionTokenMapper accountActionTokenMapper;

    @BeforeEach
    void setUp() {
        learnerProfileMapper = new LearnerProfileMapperImpl();
        oauthIdentityMapper = new OAuthIdentityMapperImpl();
        accountActionTokenMapper = new AccountActionTokenMapperImpl();
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
}
