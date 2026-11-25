package com.ieltspath.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import com.ieltspath.user.application.port.PasswordPolicySettings;

@ConfigurationProperties(prefix = "app.auth.password")
public record AuthPasswordProperties(int minLength, int bcryptCost) implements PasswordPolicySettings {
    public AuthPasswordProperties {
        if (minLength == 0) minLength = 8;
        if (bcryptCost == 0) bcryptCost = 12;
        if (minLength < 8 || minLength > 72) {
            throw new IllegalArgumentException("Độ dài tối thiểu của mật khẩu phải từ 8 đến 72 ký tự.");
        }
        if (bcryptCost < 10 || bcryptCost > 14) {
            throw new IllegalArgumentException("Chi phí BCrypt phải từ 10 đến 14.");
        }
    }
}
