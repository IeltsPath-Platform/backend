package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.LoginContext;
import com.ieltspath.user.application.exception.GoogleAccountNotActiveException;
import com.ieltspath.user.application.port.GoogleIdTokenVerifier;
import com.ieltspath.user.application.result.GoogleSignInResult;
import com.ieltspath.user.domain.exception.EmailAlreadyExistsException;
import com.ieltspath.user.domain.vo.LoginFailureReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleSignInUseCase {
    private final GoogleIdTokenVerifier verifier;
    private final LinkGoogleAccountUseCase linkGoogleAccountUseCase;
    private final LoginHistoryRecorder loginHistoryRecorder;

    public GoogleSignInResult execute(String idToken, LoginContext context) {
        var google = verifier.verify(idToken);
        LoginContext auditContext = context == null ? LoginContext.UNKNOWN : context;
        try {
            GoogleSignInResult result;
            try {
                result = linkGoogleAccountUseCase.execute(google);
            } catch (DataIntegrityViolationException | EmailAlreadyExistsException conflict) {
                result = linkGoogleAccountUseCase.execute(google);
            }
            try {
                loginHistoryRecorder.recordSuccess(result.userId(), auditContext);
            } catch (RuntimeException auditFailure) {
                log.warn("Google sign-in success audit failed userId={} errorType={}",
                        result.userId(), auditFailure.getClass().getSimpleName());
            }
            return result;
        } catch (GoogleAccountNotActiveException inactive) {
            try {
                loginHistoryRecorder.recordFailure(inactive.userId(),
                        LoginFailureReason.forInactiveStatus(inactive.status()), auditContext);
            } catch (RuntimeException auditFailure) {
                log.warn("Google sign-in failure audit failed userId={} errorType={}",
                        inactive.userId(), auditFailure.getClass().getSimpleName());
            }
            throw inactive;
        }
    }
}
