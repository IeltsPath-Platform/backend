package com.ieltspath.content.infrastructure.client;

import com.ieltspath.content.application.exception.VocabularyRejectedException;
import com.ieltspath.content.application.exception.VocabularyServiceUnavailableException;
import com.ieltspath.content.domain.vo.CourseDraftStructure.NewWord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.http.HttpMethod;

class LibraryVocabularySenseClientTest {
    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test void requestsSenseBatchWithBearerAndReturnsIds() {
        UUID senseId = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl("http://library.test").build();
        LibraryVocabularySenseClient client = new LibraryVocabularySenseClient(restClient);
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", "author").build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        server.expect(requestTo("http://library.test/api/content/vocabulary/senses?ids=" + senseId))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess("[{\"senseId\":\"" + senseId + "\"}]", MediaType.APPLICATION_JSON));

        assertThat(client.findActiveSenseIds(List.of(senseId))).containsExactly(senseId);
        server.verify();
    }

    private static NewWord word(String lemma) {
        return new NewWord(lemma, null, "NOUN", "a general direction", "xu hướng", "There is an upward trend.");
    }

    private static void signIn() {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", "author").build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test void sendsReviewedWordsInOneCallAndReturnsTheirSensesByLemma() {
        UUID senseId = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LibraryVocabularySenseClient client = new LibraryVocabularySenseClient(builder.baseUrl("http://library.test").build());
        signIn();
        server.expect(requestTo("http://library.test/api/content/admin/vocabulary/bulk"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(jsonPath("$.entries[0].lemma").value("Trend"))
                .andExpect(jsonPath("$.entries[0].partOfSpeech").value("NOUN"))
                .andExpect(jsonPath("$.entries[0].vietnameseMeaning").value("xu hướng"))
                .andRespond(withSuccess("[{\"lemma\":\"trend\",\"senseId\":\"" + senseId + "\",\"created\":true}]",
                        MediaType.APPLICATION_JSON));

        assertThat(client.resolveOrCreate(List.of(word("Trend")))).containsExactly(entry("trend", senseId));
        server.verify();
    }

    @Test void doesNotCallTheLibraryWhenThereAreNoWords() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LibraryVocabularySenseClient client = new LibraryVocabularySenseClient(builder.baseUrl("http://library.test").build());

        assertThat(client.resolveOrCreate(List.of())).isEmpty();
        server.verify();
    }

    @Test void aRefusalFromTheLibraryCarriesItsMessage() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LibraryVocabularySenseClient client = new LibraryVocabularySenseClient(builder.baseUrl("http://library.test").build());
        signIn();
        server.expect(requestTo("http://library.test/api/content/admin/vocabulary/bulk"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Vocabulary item is inactive: going\"}"));

        assertThatThrownBy(() -> client.resolveOrCreate(List.of(word("going"))))
                .isInstanceOf(VocabularyRejectedException.class).hasMessage("Vocabulary item is inactive: going");
    }

    @Test void aLibraryOutageIsReportedAsUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LibraryVocabularySenseClient client = new LibraryVocabularySenseClient(builder.baseUrl("http://library.test").build());
        signIn();
        server.expect(requestTo("http://library.test/api/content/admin/vocabulary/bulk"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.resolveOrCreate(List.of(word("trend"))))
                .isInstanceOf(VocabularyServiceUnavailableException.class);
    }

    @Test void writingWithoutASignedInAuthorIsRefused() {
        LibraryVocabularySenseClient client = new LibraryVocabularySenseClient(RestClient.builder().baseUrl("http://library.test").build());

        assertThatThrownBy(() -> client.resolveOrCreate(List.of(word("trend"))))
                .isInstanceOf(VocabularyServiceUnavailableException.class);
    }
}
