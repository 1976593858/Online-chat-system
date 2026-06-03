package com.onlinechat.security;

import com.onlinechat.config.JwtProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    @Test
    void generateTokenAndParseUserId() {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("online-chat-test");
        properties.setSecret("online-chat-system-jwt-secret-key-for-test");
        properties.setExpirationMillis(3600_000L);
        JwtTokenProvider provider = new JwtTokenProvider(properties);

        String token = provider.generateToken(1001L, "alice");

        assertThat(provider.parseUserId(token)).isEqualTo(1001L);
    }
}
