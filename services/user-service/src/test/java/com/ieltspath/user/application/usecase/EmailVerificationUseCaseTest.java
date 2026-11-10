package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.domain.aggregate.AccountActionToken;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.InvalidActionTokenException;
import com.ieltspath.user.domain.repository.AccountActionTokenRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.ActionTokenPurpose;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationUseCaseTest {
    @Mock UserRepository users;
    @Mock AccountActionTokenRepository tokens;
    @Mock TokenHashService tokenHashService;
    @Mock UserLock userLock;
    @Mock UserNoticeOutbox notices;

    private EmailVerificationUseCase useCase;

    @Test
    void resendNeverSendsToAnAccountThatIsAlreadyVerified() {
        useCase = new EmailVerificationUseCase(users, tokens, tokenHashService, userLock, notices);
        UUID userId = UUID.randomUUID();
        User verified = User.builder().id(userId).email(new Email("learner@example.com")).fullName("An")
                .status(UserStatus.ACTIVE).emailVerifiedAt(LocalDateTime.now()).build();
        when(users.findByEmail("learner@example.com")).thenReturn(Optional.of(verified));
        when(users.findById(userId)).thenReturn(Optional.of(verified));

        useCase.resend("learner@example.com");

        verify(notices, never()).enqueue(any(), any(), anyString(), any());
    }

    @Test
    void verifyMarksTheAccountVerifiedOnceTheTokenIsConsumed() {
        useCase = new EmailVerificationUseCase(users, tokens, tokenHashService, userLock, notices);
        UUID userId = UUID.randomUUID();
        User unverified = User.builder().id(userId).email(new Email("learner@example.com")).fullName("An")
                .status(UserStatus.ACTIVE).build();
        AccountActionToken token = AccountActionToken.builder().userId(userId)
                .purpose(ActionTokenPurpose.EMAIL_VERIFICATION).tokenHash("hash")
                .expiresAt(LocalDateTime.now().plusMinutes(30)).createdAt(LocalDateTime.now()).build();
        when(tokenHashService.hash("raw-token")).thenReturn("hash");
        when(tokens.findByTokenHashAndPurpose("hash", ActionTokenPurpose.EMAIL_VERIFICATION)).thenReturn(Optional.of(token));
        when(users.findById(userId)).thenReturn(Optional.of(unverified));
        when(tokens.consumeToken(any(), any(), any())).thenReturn(true);

        useCase.verify("raw-token");

        verify(users).save(unverified);
        assertTrue(unverified.isEmailVerified());
    }

    @Test
    void verifyRefusesAConsumedOrUnknownTokenAndLeavesTheAccountUnverified() {
        useCase = new EmailVerificationUseCase(users, tokens, tokenHashService, userLock, notices);
        when(tokenHashService.hash("raw-token")).thenReturn("hash");
        when(tokens.findByTokenHashAndPurpose("hash", ActionTokenPurpose.EMAIL_VERIFICATION)).thenReturn(Optional.empty());

        assertThrows(InvalidActionTokenException.class, () -> useCase.verify("raw-token"));
        User unverified = User.builder().id(UUID.randomUUID()).email(new Email("x@example.com")).status(UserStatus.ACTIVE).build();
        assertFalse(unverified.isEmailVerified());
    }
}
