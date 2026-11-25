package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.user.api.cookie.AuthCookieService;
import com.ieltspath.user.application.port.GoogleNonceStore;
import com.ieltspath.user.application.usecase.AuditedLoginUseCase;
import com.ieltspath.user.application.usecase.EmailVerificationUseCase;
import com.ieltspath.user.application.usecase.ForgotPasswordUseCase;
import com.ieltspath.user.application.usecase.GetUserByIdUseCase;
import com.ieltspath.user.application.usecase.GoogleSignInUseCase;
import com.ieltspath.user.application.usecase.LogoutUseCase;
import com.ieltspath.user.application.usecase.RefreshTokenUseCase;
import com.ieltspath.user.application.usecase.ResetPasswordUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
        "app.auth.external-jwt-issuer=urn:code-base:auth",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "app.security.public-endpoints[0].method=POST",
        "app.security.public-endpoints[0].patterns[0]=/auth/google/nonce",
        "spring.cloud.config.enabled=false"
})
class GoogleNonceControllerTest {
    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";

    @Autowired MockMvc mvc;
    @MockBean GoogleNonceStore nonceStore;
    @MockBean AuditedLoginUseCase loginUseCase;
    @MockBean GoogleSignInUseCase googleSignInUseCase;
    @MockBean RefreshTokenUseCase refreshTokenUseCase;
    @MockBean LogoutUseCase logoutUseCase;
    @MockBean ForgotPasswordUseCase forgotPasswordUseCase;
    @MockBean ResetPasswordUseCase resetPasswordUseCase;
    @MockBean EmailVerificationUseCase emailVerificationUseCase;
    @MockBean GetUserByIdUseCase getUserByIdUseCase;
    @MockBean CurrentUserProvider currentUserProvider;
    @MockBean AuthCookieService authCookieService;

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> INTERNAL_SECRET);
    }

    @Test
    void issuesNonceWithoutAuthenticationAndPreventsCaching() throws Exception {
        when(nonceStore.issue()).thenReturn("n".repeat(43));

        mvc.perform(post("/auth/google/nonce").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.nonce").value("n".repeat(43)))
                .andExpect(jsonPath("$.expiresInSeconds").value(300));
    }
}
