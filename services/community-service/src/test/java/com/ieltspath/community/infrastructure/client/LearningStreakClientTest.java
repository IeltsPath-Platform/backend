package com.ieltspath.community.infrastructure.client;

import com.ieltspath.community.domain.exception.CommunityUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LearningStreakClientTest {
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final LearningStreakClient client = new LearningStreakClient(builder.baseUrl("http://learning.test").build());

    @Test
    void asksLearningOnceForTheWholeBatchAndMapsTheAnswerByLearner() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        server.expect(requestTo("http://learning.test/internal/learning/streaks"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.userIds.length()").value(2))
                .andRespond(withSuccess("{\"streaks\":[{\"userId\":\"" + first + "\",\"currentDays\":7}]}",
                        MediaType.APPLICATION_JSON));

        Map<UUID, Integer> result = client.streaks(Set.of(first, second));

        assertThat(result).containsOnly(Map.entry(first, 7));
        server.verify();
    }

    @Test
    void doesNotCallLearningForNoLearners() {
        assertThat(client.streaks(Set.of())).isEmpty();
        server.verify();
    }

    @Test
    void reportsLearningAsUnavailableWhenItFails() {
        server.expect(requestTo("http://learning.test/internal/learning/streaks")).andRespond(withServerError());

        assertThatThrownBy(() -> client.streaks(Set.of(UUID.randomUUID())))
                .isInstanceOf(CommunityUnavailableException.class);
    }
}
