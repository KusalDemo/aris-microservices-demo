package com.aris.common.security;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import javax.crypto.SecretKey;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Issue and decode HS256 JWTs for the ARIS demo.
 */
public class ArisJwtService {

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_NAME = "name";
    public static final String CLAIM_ROLE = "role";

    private final JwtProperties properties;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public ArisJwtService(JwtProperties properties) {
        this.properties = Objects.requireNonNull(properties);
        SecretKey key = JwtKeys.hmacSha256Key(properties.getSecret());
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key.getEncoded()));
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    public ArisJwtService(JwtProperties properties, JwtEncoder encoder, JwtDecoder decoder) {
        this.properties = properties;
        this.encoder = encoder;
        this.decoder = decoder;
    }

    public String issueToken(String userId, String email, String name, String role) {
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(properties.getExpirySeconds());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .issuedAt(now)
                .expiresAt(expires)
                .subject(userId)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_NAME, name)
                .claim(CLAIM_ROLE, role)
                .claim("roles", List.of(role))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Jwt decode(String token) {
        return decoder.decode(token);
    }

    public JwtDecoder jwtDecoder() {
        return decoder;
    }

    public long getExpirySeconds() {
        return properties.getExpirySeconds();
    }

    public static JwtDecoder createDecoder(JwtProperties properties) {
        SecretKey key = JwtKeys.hmacSha256Key(properties.getSecret());
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
