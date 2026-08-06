package com.aris.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Shared JWT settings ({@code jwt.*} / {@code JWT_*} env vars).
 */
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * HS256 secret — must be at least 32 characters for demo safety.
     */
    private String secret = "aris-demo-jwt-secret-change-me-32chars";

    private String issuer = "aris-auth";

    private long expirySeconds = 3600L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getExpirySeconds() {
        return expirySeconds;
    }

    public void setExpirySeconds(long expirySeconds) {
        this.expirySeconds = expirySeconds;
    }
}
