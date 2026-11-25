package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.LoginContext;
import com.ieltspath.user.application.exception.GoogleAccountNotActiveException;
import com.ieltspath.user.application.port.GoogleIdTokenVerifier;
import com.ieltspath.user.application.port.GoogleSignInMetrics;
import com.ieltspath.user.application.exception.GoogleAccountConflictException;
import com.ieltspath.user.application.exception.GoogleEmailNotSupportedException;
import com.ieltspath.user.application.exception.GoogleSignInNotAllowedException;
import com.ieltspath.user.application.exception.GoogleSignInUnavailableException;
import com.ieltspath.user.domain.exception.AuthenticationFailedException;
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
    private final GoogleSignInMetrics metrics;

    public GoogleSignInResult execute(String idToken, LoginContext context) {
        try {
            GoogleSignInResult result = executeAndAudit(idToken, context);
            metrics.record(GoogleSignInMetrics.Result.SUCCESS);
            return result;
        } catch (RuntimeException failure) {
            GoogleSignInMetrics.Result result = classify(failure);
            if (result != null) metrics.record(result);
            throw failure;
        }
    }

    private GoogleSignInResult executeAndAudit(String idToken, LoginContext context) {
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

    private GoogleSignInMetrics.Result classify(RuntimeException failure) {
        if (failure instanceof GoogleEmailNotSupportedException) return GoogleSignInMetrics.Result.EMAIL_NOT_SUPPORTED;
        if (failure instanceof GoogleSignInNotAllowedException) return GoogleSignInMetrics.Result.NOT_ALLOWED;
        if (failure instanceof GoogleAccountConflictException || failure instanceof EmailAlreadyExistsException
                || failure instanceof DataIntegrityViolationException) return GoogleSignInMetrics.Result.CONFLICT;
        if (failure instanceof GoogleAccountNotActiveException) return GoogleSignInMetrics.Result.INACTIVE;
        if (failure instanceof GoogleSignInUnavailableException) return GoogleSignInMetrics.Result.UNAVAILABLE;
        if (failure instanceof AuthenticationFailedException) return GoogleSignInMetrics.Result.INVALID_TOKEN;
        return null;
    }
}
