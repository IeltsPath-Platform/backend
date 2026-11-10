package com.ieltspath.user.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.user.api.cookie.AuthCookieService;
import com.ieltspath.user.application.command.ForgotPasswordCommand;
import com.ieltspath.user.application.command.LoginContext;
import com.ieltspath.user.application.command.ResetPasswordCommand;
import com.ieltspath.user.application.result.AuthTokenResult;
import com.ieltspath.user.application.result.GoogleSignInResult;
import com.ieltspath.user.application.usecase.ForgotPasswordUseCase;
import com.ieltspath.user.application.usecase.GoogleSignInUseCase;
import com.ieltspath.user.application.usecase.GetUserByIdUseCase;
import com.ieltspath.user.application.usecase.AuditedLoginUseCase;
import com.ieltspath.user.application.usecase.LogoutUseCase;
import com.ieltspath.user.application.usecase.RefreshTokenUseCase;
import com.ieltspath.user.application.usecase.ResetPasswordUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
    "app.auth.external-jwt-issuer=urn:code-base:auth",
    "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
    "app.security.public-endpoints[0].method=POST",
    "app.security.public-endpoints[0].patterns[0]=/auth/login",
    "app.security.public-endpoints[0].patterns[1]=/auth/refresh",
    "app.security.public-endpoints[0].patterns[2]=/auth/logout",
    "app.security.public-endpoints[0].patterns[3]=/auth/forgot-password",
    "app.security.public-endpoints[0].patterns[4]=/auth/reset-password",
    "app.security.public-endpoints[0].patterns[5]=/auth/google",
    "spring.cloud.config.enabled=false"
})
class ActionTokenSecurityTest {

    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditedLoginUseCase loginUseCase;
    @MockBean
    private GoogleSignInUseCase googleSignInUseCase;
    @MockBean
    private RefreshTokenUseCase refreshTokenUseCase;
    @MockBean
    private LogoutUseCase logoutUseCase;
    @MockBean
    private ForgotPasswordUseCase forgotPasswordUseCase;
    @MockBean
    private ResetPasswordUseCase resetPasswordUseCase;
    @MockBean
    private GetUserByIdUseCase getUserByIdUseCase;
    @MockBean
    private AuthCookieService authCookieService;

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> INTERNAL_SECRET);
    }

    @Test
    void forgotPasswordIsPublic() throws Exception {
        String body = """
                {
                    "email": "test@example.com"
                }
                """;

        mockMvc.perform(post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Nếu email tồn tại, hướng dẫn đặt lại mật khẩu sẽ được gửi."));

        verify(forgotPasswordUseCase).execute(any(ForgotPasswordCommand.class));
    }

    @Test
    void forgotPasswordAcceptsInvalidEmailAndReturnsSameGenericResponse() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Nếu email tồn tại, hướng dẫn đặt lại mật khẩu sẽ được gửi."));
    }

    @Test
    void resetPasswordIsPublic() throws Exception {
        String body = """
                {
                    "token": "valid-token",
                    "newPassword": "newpassword123"
                }
                """;

        mockMvc.perform(post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đặt lại mật khẩu thành công"));

        verify(resetPasswordUseCase).execute(any(ResetPasswordCommand.class));
    }

    @Test
    void googleSignInIsPublicAndReturnsTheLoginTokenShape() throws Exception {
        AuthTokenResult tokens = new AuthTokenResult("access", "refresh", 3600, 604800);
        when(googleSignInUseCase.execute(eq("signed-id-token"), any(LoginContext.class)))
                .thenReturn(new GoogleSignInResult(UUID.randomUUID(), tokens));

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"signed-id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
        verify(authCookieService).addAuthCookies(any(), eq("access"), eq("refresh"), eq(3600L), eq(604800L));
    }

    @Test
    void googleSignInRequiresAnIdToken() throws Exception {
        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
