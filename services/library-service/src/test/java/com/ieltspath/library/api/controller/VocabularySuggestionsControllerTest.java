package com.ieltspath.library.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.library.application.query.VocabularySuggestionsQuery;
import com.ieltspath.library.application.result.VocabularySuggestionsResult;
import com.ieltspath.library.application.usecase.SuggestVocabularyUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VocabularySuggestionsController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "app.auth.internal-jwt-secret=ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=",
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false"
})
class VocabularySuggestionsControllerTest {
    private static final String PATH = "/api/content/admin/vocabulary/suggestions";
    @Autowired MockMvc mvc;
    @MockBean SuggestVocabularyUseCase suggest;

    @Test void requiresContentAuthor() throws Exception {
        mvc.perform(get(PATH).param("q", "poll")).andExpect(status().isUnauthorized());
        for (String role : List.of("CUSTOMER", "ADMIN")) {
            mvc.perform(get(PATH).param("q", "poll").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role))))
                    .andExpect(status().isForbidden());
        }
        when(suggest.execute(any())).thenReturn(new VocabularySuggestionsResult(List.of(), 0, 20, false));
        mvc.perform(get(PATH).param("q", "poll").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CONTENT_AUTHOR"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test void rejectsOversizedQueriesAndInvalidPagination() throws Exception {
        var author = jwt().authorities(new SimpleGrantedAuthority("ROLE_CONTENT_AUTHOR"));
        mvc.perform(get(PATH).param("q", "x".repeat(101)).with(author)).andExpect(status().isBadRequest());
        mvc.perform(get(PATH).param("q", "poll").param("size", "51").with(author)).andExpect(status().isBadRequest());
        mvc.perform(get(PATH).param("q", "poll").param("size", "0").with(author)).andExpect(status().isBadRequest());
        mvc.perform(get(PATH).param("q", "poll").param("page", "-1").with(author)).andExpect(status().isBadRequest());
    }

    @Test void returnsRetryableErrorOnDatabaseTimeout() throws Exception {
        when(suggest.execute(new VocabularySuggestionsQuery("poll", 0, 20)))
                .thenThrow(new org.springframework.dao.QueryTimeoutException("deadline"));
        mvc.perform(get(PATH).param("q", "poll").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CONTENT_AUTHOR"))))
                .andExpect(status().isServiceUnavailable());
    }
}
