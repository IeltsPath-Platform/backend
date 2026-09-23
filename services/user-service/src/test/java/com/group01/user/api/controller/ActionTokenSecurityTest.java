package com.group01.user.api.controller;

import com.group01.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.group01.user.api.cookie.AuthCookieService;
import com.group01.user.application.command.ForgotPasswordCommand;
import com.group01.user.application.command.ResetPasswordCommand;
import com.group01.user.application.usecase.ForgotPasswordUseCase;
import com.group01.user.application.usecase.GetUserByIdUseCase;
import com.group01.user.application.usecase.LoginUseCase;
import com.group01.user.application.usecase.LogoutUseCase;
import com.group01.user.application.usecase.RefreshTokenUseCase;
import com.group01.user.application.usecase.ResetPasswordUseCase;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    "spring.cloud.config.enabled=false"
})
class ActionTokenSecurityTest {

    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoginUseCase loginUseCase;
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
        when(forgotPasswordUseCase.execute(any(ForgotPasswordCommand.class))).thenReturn("raw-token");

        String body = """
                {
                    "email": "test@example.com"
                }
                """;

        mockMvc.perform(post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mã xác nhận đặt lại mật khẩu đã được xử lý"));

        verify(forgotPasswordUseCase).execute(any(ForgotPasswordCommand.class));
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
}
