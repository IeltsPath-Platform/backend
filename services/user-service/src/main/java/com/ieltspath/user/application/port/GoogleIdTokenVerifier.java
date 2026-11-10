package com.ieltspath.user.application.port;

import com.ieltspath.user.application.result.GoogleIdentity;

public interface GoogleIdTokenVerifier {
    GoogleIdentity verify(String idToken);
}
