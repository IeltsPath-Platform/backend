package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.exception.GoogleAccountConflictException;
import com.ieltspath.user.application.exception.GoogleAccountNotActiveException;
import com.ieltspath.user.application.exception.GoogleEmailNotSupportedException;
import com.ieltspath.user.application.exception.GoogleSignInNotAllowedException;
import com.ieltspath.user.application.port.UserLock;
import com.ieltspath.user.application.result.AuthTokenResult;
import com.ieltspath.user.application.result.GoogleIdentity;
import com.ieltspath.user.application.result.GoogleSignInResult;
import com.ieltspath.user.domain.aggregate.OAuthIdentity;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.exception.RoleNotFoundException;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.OAuthIdentityRepository;
import com.ieltspath.user.domain.repository.RefreshTokenRepository;
import com.ieltspath.user.domain.repository.RoleRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.FullName;
import com.ieltspath.user.domain.vo.OAuthProvider;
import com.ieltspath.user.domain.vo.PhoneNumber;
import com.ieltspath.user.domain.vo.RoleName;
import com.ieltspath.user.domain.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LinkGoogleAccountUseCase {
    private final UserRepository userRepository;
    private final OAuthIdentityRepository oauthIdentityRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserLock userLock;
    private final AuthTokenIssuer authTokenIssuer;

    @Transactional
    public GoogleSignInResult execute(GoogleIdentity google) {
        if (!google.isEmailAuthoritative()) {
            throw new GoogleEmailNotSupportedException();
        }
        String email = new Email(google.email()).value();
        userLock.lockCreationKey("google-subject:" + google.subject());
        OAuthIdentity identity = oauthIdentityRepository
                .findByProviderAndProviderSubject(OAuthProvider.GOOGLE, google.subject())
                .orElse(null);
        User user;
        if (identity != null) {
            user = lockAndReload(identity.getUserId());
            identity = oauthIdentityRepository.findByUserIdAndProvider(user.getId(), OAuthProvider.GOOGLE)
                    .orElseThrow(GoogleAccountConflictException::new);
            if (!identity.getProviderSubject().equals(google.subject())) {
                throw new GoogleAccountConflictException();
            }
        } else {
            userLock.lockCreationKey("email:" + email);
            user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                user = createGoogleUser(google, email);
            } else {
                user = lockAndReload(user.getId());
                identity = oauthIdentityRepository.findByUserIdAndProvider(user.getId(), OAuthProvider.GOOGLE)
                        .orElse(null);
                if (identity != null && !identity.getProviderSubject().equals(google.subject())) {
                    throw new GoogleAccountConflictException();
                }
            }
        }

        requireGoogleEligible(user);
        LocalDateTime now = LocalDateTime.now();
        if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
            userRepository.clearPasswordHash(user.getId());
            user.clearPassword();
            refreshTokenRepository.revokeAllActiveForUser(user.getId(), now);
        }
        if (identity == null) {
            identity = OAuthIdentity.builder()
                    .userId(user.getId())
                    .provider(OAuthProvider.GOOGLE)
                    .providerSubject(google.subject())
                    .linkedAt(now)
                    .build();
        }
        identity.recordAuthentication();
        oauthIdentityRepository.save(identity);
        AuthTokenResult tokens = authTokenIssuer.issue(user);
        return new GoogleSignInResult(user.getId(), tokens);
    }

    private User createGoogleUser(GoogleIdentity google, String email) {
        Set<String> roleNames = Set.of(RoleName.CUSTOMER.name());
        var roles = roleRepository.findByNames(roleNames);
        if (roles.isEmpty()) {
            throw new RoleNotFoundException("Vai trò CUSTOMER không tồn tại");
        }
        User user = User.builder()
                .email(new Email(email))
                .passwordHash(null)
                .fullName(resolveFullName(google.name(), email))
                .phoneNumber(PhoneNumber.parse(null))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(roles))
                .build();
        User saved = userRepository.save(user);
        return lockAndReload(saved.getId());
    }

    private String resolveFullName(String suppliedName, String email) {
        if (suppliedName != null && !suppliedName.isBlank()) {
            try {
                return new FullName(suppliedName).value();
            } catch (IllegalArgumentException ignored) {
                // Use the email's local part when Google's display name is not valid for the user table.
            }
        }
        return new FullName(email.substring(0, email.indexOf('@'))).value();
    }

    private User lockAndReload(UUID userId) {
        userLock.lock(userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng: " + userId));
    }

    private void requireGoogleEligible(User user) {
        if (user.getRoles().stream().anyMatch(role -> role.getName() != RoleName.CUSTOMER)) {
            throw new GoogleSignInNotAllowedException();
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new GoogleAccountNotActiveException(user.getId(), user.getStatus());
        }
    }
}
