package com.ieltspath.community.infrastructure.client;

import com.ieltspath.community.application.port.StreakLookup;
import com.ieltspath.community.application.result.PublicProfile;
import com.ieltspath.community.domain.exception.CommunityUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Public profiles come from User; the streak beside each one comes from Learning, one call for the whole batch. */
class UserPublicProfileClientTest {
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer user = MockRestServiceServer.bindTo(builder).build();
    private final AtomicInteger streakCalls = new AtomicInteger();

    private UserPublicProfileClient clientWith(StreakLookup streaks) {
        return new UserPublicProfileClient(builder.baseUrl("http://user.test").build(), streaks);
    }

    private void userAnswersWithProfilesOfBoth(String path) {
        user.expect(requestTo("http://user.test" + path)).andRespond(withSuccess("""
                {"profiles":[{"id":"%s","displayName":"An","streak":99},{"id":"%s","displayName":"Binh"}]}
                """.formatted(first, second), MediaType.APPLICATION_JSON));
    }

    @Test
    void takesTheStreakFromLearningAndIgnoresAnyStreakUserStillSends() {
        userAnswersWithProfilesOfBoth("/internal/users/public-profiles");
        var client = clientWith(ids -> { streakCalls.incrementAndGet(); return Map.of(first, 4, second, 11); });

        Map<UUID, PublicProfile> result = client.findProfiles(Set.of(first, second));

        assertThat(result.get(first).streak()).isEqualTo(4);
        assertThat(result.get(second).streak()).isEqualTo(11);
        assertThat(streakCalls).hasValue(1);
    }

    @Test
    void aLearnerLearningHasNoStreakForShowsZero() {
        userAnswersWithProfilesOfBoth("/internal/users/public-profiles");
        var client = clientWith(ids -> Map.of(first, 3));

        assertThat(client.findProfiles(Set.of(first, second)).get(second).streak()).isZero();
    }

    @Test
    void keepsTheProfilesAndHidesTheStreakWhenLearningIsUnavailable() {
        userAnswersWithProfilesOfBoth("/internal/users/public-profiles");
        StreakLookup down = ids -> { throw new CommunityUnavailableException("down"); };

        Map<UUID, PublicProfile> result = clientWith(down).findProfiles(Set.of(first, second));

        assertThat(result).containsOnlyKeys(first, second);
        assertThat(result.get(first).displayName()).isEqualTo("An");
        assertThat(result.get(first).streak()).isNull();
        assertThat(result.get(second).streak()).isNull();
    }

    @Test
    void searchResultsGetTheirStreakFromLearningToo() {
        user.expect(requestTo("http://user.test/internal/users/public-profiles/search?q=an&limit=5"))
                .andRespond(withSuccess("{\"profiles\":[{\"id\":\"%s\",\"displayName\":\"An\"}]}".formatted(first),
                        MediaType.APPLICATION_JSON));
        var client = clientWith(ids -> Map.of(first, 8));

        List<PublicProfile> result = client.searchProfiles(UUID.randomUUID(), "an", 5);

        assertThat(result).singleElement().satisfies(profile -> assertThat(profile.streak()).isEqualTo(8));
    }

    @Test
    void doesNotAskLearningWhenThereAreNoProfiles() {
        user.expect(requestTo("http://user.test/internal/users/public-profiles/search?q=zz&limit=5"))
                .andRespond(withSuccess("{\"profiles\":[]}", MediaType.APPLICATION_JSON));
        var client = clientWith(ids -> { streakCalls.incrementAndGet(); return Map.of(); });

        assertThat(client.searchProfiles(UUID.randomUUID(), "zz", 5)).isEmpty();
        assertThat(streakCalls).hasValue(0);
    }
}
