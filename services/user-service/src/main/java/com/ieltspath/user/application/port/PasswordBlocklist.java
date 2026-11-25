package com.ieltspath.user.application.port;

public interface PasswordBlocklist {
    boolean contains(String password);
}
