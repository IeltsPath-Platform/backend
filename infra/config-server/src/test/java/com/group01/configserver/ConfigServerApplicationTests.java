package com.group01.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "encrypt.key=test-config-server-encrypt-key",
        "spring.cloud.config.server.git.clone-on-start=false"
})
class ConfigServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
