package com.aris.common.security;

import javax.crypto.SecretKey;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Factory helpers for servlet JWT decoders (shared HS256 secret).
 */
public final class JwtDecoders {

    private JwtDecoders() {
    }

    public static JwtDecoder servletDecoder(JwtProperties properties) {
        SecretKey key = JwtKeys.hmacSha256Key(properties.getSecret());
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
