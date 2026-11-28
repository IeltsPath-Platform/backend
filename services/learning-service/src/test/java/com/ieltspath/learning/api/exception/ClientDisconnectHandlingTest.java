package com.ieltspath.learning.api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class ClientDisconnectHandlingTest {

    @RestController
    static class AbandonedController {
        @GetMapping("/abandoned")
        String abandoned() throws AsyncRequestNotUsableException {
            throw new AsyncRequestNotUsableException("client has gone");
        }
    }

    @Test
    void writesNoErrorResponseWhenTheClientHasLeft() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AbandonedController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        MvcResult result = mvc.perform(get("/abandoned")).andReturn();

        assertThat(result.getResolvedException()).isInstanceOf(AsyncRequestNotUsableException.class);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).isEmpty();
    }
}
