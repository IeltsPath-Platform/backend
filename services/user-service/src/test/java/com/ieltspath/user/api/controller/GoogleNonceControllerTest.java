package com.ieltspath.user.api.controller;

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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class GoogleNonceControllerTest {
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
