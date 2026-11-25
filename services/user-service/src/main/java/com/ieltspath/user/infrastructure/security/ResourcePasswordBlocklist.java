package com.ieltspath.user.infrastructure.security;

import com.ieltspath.user.application.port.PasswordBlocklist;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ResourcePasswordBlocklist implements PasswordBlocklist {
    private final Set<String> blocked;

    public ResourcePasswordBlocklist() {
        try (var input = new ClassPathResource("password-blocklist.txt").getInputStream()) {
            blocked = new String(input.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::trim).filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(line -> line.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tải danh sách mật khẩu bị chặn.", exception);
        }
    }

    @Override
    public boolean contains(String password) {
        return password != null && blocked.contains(password.toLowerCase(Locale.ROOT));
    }
}
