package com.ieltspath.content.infrastructure.client;

import com.ieltspath.content.application.port.ContentGenerationException;
import com.ieltspath.content.application.result.GenerationReply;
import com.ieltspath.content.infrastructure.config.ContentAiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class OpenAiCompatibleContentGeneratorTest {

    private static final String BASE = "http://llm.test/v1";

    @Test
    void missingConfigurationFailsWithoutCallingAnything() {
        ContentAiProperties properties = properties("", "");
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        var generator = new OpenAiCompatibleContentGenerator(builder.build(), properties);

        ContentGenerationException failure = assertThrows(ContentGenerationException.class,
                () -> generator.generate("prompt"));
        assertEquals(ContentGenerationException.NOT_CONFIGURED, failure.code());
        server.verify();
    }

    @Test
    void successfulReplyReturnsItsTextAndModel() {
        ContentAiProperties properties = properties(BASE, "m-1");
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + "/chat/completions")).andExpect(method(POST))
                .andRespond(withSuccess("""
                        {"model":"m-1","choices":[{"message":{"role":"assistant","content":"{\\"ok\\":true}"}}]}
                        """, APPLICATION_JSON));

        GenerationReply reply = new OpenAiCompatibleContentGenerator(builder.build(), properties).generate("prompt");

        assertEquals("{\"ok\":true}", reply.text());
        assertEquals("m-1", reply.model());
        server.verify();
    }

    @Test
    void serverErrorBecomesAFailureCodeWithoutTheBody() {
        ContentAiProperties properties = properties(BASE, "m-1");
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + "/chat/completions"))
                .andRespond(withServerError().body("secret detail"));

        ContentGenerationException failure = assertThrows(ContentGenerationException.class,
                () -> new OpenAiCompatibleContentGenerator(builder.build(), properties).generate("prompt"));
        assertEquals(ContentGenerationException.FAILED, failure.code());
        assertEquals(false, failure.getMessage().contains("secret detail"));
    }

    @Test
    void replyWithoutTextIsAnInvalidReply() {
        ContentAiProperties properties = properties(BASE, "m-1");
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + "/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[]}", APPLICATION_JSON));

        ContentGenerationException failure = assertThrows(ContentGenerationException.class,
                () -> new OpenAiCompatibleContentGenerator(builder.build(), properties).generate("prompt"));
        assertEquals(ContentGenerationException.INVALID_REPLY, failure.code());
    }

    private static ContentAiProperties properties(String baseUrl, String model) {
        ContentAiProperties properties = new ContentAiProperties();
        properties.getLlm().setBaseUrl(baseUrl);
        properties.getLlm().setModel(model);
        return properties;
    }
}
