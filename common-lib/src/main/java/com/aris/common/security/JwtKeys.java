package com.aris.common.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Shared HMAC key material for HS256 JWT issue/validate across Gateway and services.
 */
public final class JwtKeys {

    private JwtKeys() {
    }

    public static SecretKey hmacSha256Key(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            for (int i = bytes.length; i < 32; i++) {
                padded[i] = (byte) ('A' + (i % 26));
            }
            bytes = padded;
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
