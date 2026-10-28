package com.ieltspath.user.domain.aggregate;

import com.ieltspath.user.domain.vo.ProfileVisibility;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LearnerProfileTest {

    @Test
    void updateDetails_updatesFieldsAndTouchesTimestamp() {
        LearnerProfile profile = LearnerProfile.builder()
                .userId(UUID.randomUUID())
                .displayName("Old Name")
                .timezone("UTC")
                .visibility(ProfileVisibility.PUBLIC)
                .build();

        profile.updateDetails("New Name", "bio text", BigDecimal.valueOf(6.5), "Asia/Ho_Chi_Minh");

        assertThat(profile.getDisplayName()).isEqualTo("New Name");
        assertThat(profile.getBio()).isEqualTo("bio text");
        assertThat(profile.getSelfReportedBand()).isEqualByComparingTo("6.5");
        assertThat(profile.getTimezone()).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(profile.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateDetails_throwsWhenDisplayNameIsBlank() {
        LearnerProfile profile = LearnerProfile.builder()
                .userId(UUID.randomUUID())
                .displayName("Name")
                .build();

        assertThatThrownBy(() -> profile.updateDetails("", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changeVisibility_updatesVisibility() {
        LearnerProfile profile = LearnerProfile.builder()
                .userId(UUID.randomUUID())
                .displayName("Name")
                .visibility(ProfileVisibility.PUBLIC)
                .build();

        profile.changeVisibility(ProfileVisibility.PRIVATE);

        assertThat(profile.getVisibility()).isEqualTo(ProfileVisibility.PRIVATE);
        assertThat(profile.getUpdatedAt()).isNotNull();
    }

    @Test
    void createDefault_namesTheProfileAfterTheAccountAndMakesItPublic() {
        UUID id = UUID.randomUUID();

        LearnerProfile profile = LearnerProfile.createDefault(id, "  Nguyen Van A ");

        assertThat(profile.getUserId()).isEqualTo(id);
        assertThat(profile.getDisplayName()).isEqualTo("Nguyen Van A");
        assertThat(profile.getVisibility()).isEqualTo(ProfileVisibility.PUBLIC);
        assertThat(profile.getTimezone()).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(profile.getAvatarReference()).isNull();
        assertThat(LearnerProfile.createDefault(id, " ").getDisplayName()).isEqualTo("Learner");
    }

    @Test
    void updateDetails_leavesTheAvatarAlone() {
        LearnerProfile profile = LearnerProfile.createDefault(UUID.randomUUID(), "Name");
        profile.updateAvatarReference("https://cdn.example.test/avatars/images/me.png");

        profile.updateDetails("Other", "bio", null, "UTC");

        assertThat(profile.getAvatarReference()).isEqualTo("https://cdn.example.test/avatars/images/me.png");
    }
}
