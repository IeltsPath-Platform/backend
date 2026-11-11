package com.ieltspath.user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.auth.google")
public class GoogleAuthProperties {
    private List<String> clientIds = new ArrayList<>();
}
