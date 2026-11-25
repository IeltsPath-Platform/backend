package com.ieltspath.user.application.port;

import com.ieltspath.user.application.command.LoginContext;

public interface AuthAuditContextProvider {
    LoginContext current();
}
