package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.learning.domain.repository.StudySessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** The study timezone is the one on the learner's profile, kept by the User Service. */
class UserServiceStudyTimezonesTest {
    private static final ZoneId DEFAULT = ZoneId.of("Asia/Ho_Chi_Minh");
    private final UUID user = UUID.randomUUID();
    private final StudySessionRepository sessions = mock(StudySessionRepository.class);
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final UserServiceStudyTimezones timezones =
            new UserServiceStudyTimezones(builder.baseUrl("http://user.test").build(), sessions);

    @AfterEach
    void clearRequest() {
        SecurityContextHolder.clearContext();
    }

    private void signedInRequest() {
        Jwt jwt = Jwt.withTokenValue("learner-token").header("alg", "none").subject(user.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, java.util.List.of()));
    }

    private String url() {
        return "http://user.test/internal/users/" + user + "/study-timezone";
    }

    @Test
    void usesTheTimezoneOnTheProfileAndForwardsTheCallersToken() {
        signedInRequest();
        server.expect(requestTo(url())).andExpect(header("Authorization", "Bearer learner-token"))
                .andRespond(withSuccess("{\"timezone\":\"America/Los_Angeles\"}", MediaType.APPLICATION_JSON));

        assertEquals(ZoneId.of("America/Los_Angeles"), timezones.zoneOf(user));
        server.verify();
    }

    @Test
    void fallsBackToTheLastTimezoneUsedWhenTheUserServiceIsDown() {
        signedInRequest();
        server.expect(requestTo(url())).andRespond(withServerError());
        when(sessions.latestTimezone(user)).thenReturn(Optional.of("Europe/Paris"));

        assertEquals(ZoneId.of("Europe/Paris"), timezones.zoneOf(user));
    }

    @Test
    void fallsBackToTheDefaultWhenNothingIsKnown() {
        signedInRequest();
        server.expect(requestTo(url())).andRespond(withServerError());
        when(sessions.latestTimezone(user)).thenReturn(Optional.empty());

        assertEquals(DEFAULT, timezones.zoneOf(user));
    }

    @Test
    void anInvalidTimezoneNameIsIgnored() {
        signedInRequest();
        server.expect(requestTo(url()))
                .andRespond(withSuccess("{\"timezone\":\"Mars/Base\"}", MediaType.APPLICATION_JSON));
        when(sessions.latestTimezone(user)).thenReturn(Optional.of("not-a-zone"));

        assertEquals(DEFAULT, timezones.zoneOf(user));
    }

    @Test
    void withoutARequestTokenTheUserServiceIsNotAsked() {
        when(sessions.latestTimezone(user)).thenReturn(Optional.of("Asia/Tokyo"));

        assertEquals(ZoneId.of("Asia/Tokyo"), timezones.zoneOf(user));
        server.verify();
    }
}
