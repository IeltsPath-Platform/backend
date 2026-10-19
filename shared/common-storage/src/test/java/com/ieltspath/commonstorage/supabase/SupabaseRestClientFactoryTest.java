package com.ieltspath.commonstorage.supabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ieltspath.commonstorage.config.SupabaseStorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SupabaseRestClientFactoryTest {

    private static final String BASE = "https://test-ref.supabase.co/storage/v1";

    private static MockRestServiceServer serverFor(RestClient.Builder builder) {
        return MockRestServiceServer.bindTo(builder).build();
    }

    @Test
    void secretKeyGoesOnlyInApikeyHeader() {
        var props = new SupabaseStorageProperties("https://test-ref.supabase.co", "sb_secret_test-not-real",
                null, null, null);
        RestClient.Builder builder = RestClient.builder();
        var server = serverFor(builder);
        RestClient client = SupabaseRestClientFactory.configure(builder, props).build();

        server.expect(requestTo(BASE + "/bucket"))
                .andExpect(header("apikey", "sb_secret_test-not-real"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        client.get().uri("/bucket").retrieve().toBodilessEntity();
        server.verify();
    }

    @Test
    void legacyJwtKeyGoesInBothHeaders() {
        var props = new SupabaseStorageProperties("https://test-ref.supabase.co", "eyJ.test.not-real",
                null, null, null);
        RestClient.Builder builder = RestClient.builder();
        var server = serverFor(builder);
        RestClient client = SupabaseRestClientFactory.configure(builder, props).build();

        server.expect(requestTo(BASE + "/bucket"))
                .andExpect(header("apikey", "eyJ.test.not-real"))
                .andExpect(header("Authorization", "Bearer eyJ.test.not-real"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        client.get().uri("/bucket").retrieve().toBodilessEntity();
        server.verify();
    }

    @Test
    void timeoutsAndRedirectPolicyAreApplied() {
        var props = new SupabaseStorageProperties("https://test-ref.supabase.co", "sb_secret_test-not-real",
                null, null, null);
        var http = SupabaseRestClientFactory.httpClient(props);
        assertThat(http.followRedirects()).isEqualTo(java.net.http.HttpClient.Redirect.NEVER);
        assertThat(http.connectTimeout()).contains(props.connectTimeout());
        http.close();
    }
}
