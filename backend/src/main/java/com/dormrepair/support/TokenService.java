package com.dormrepair.support;
// AI-assisted: JWT signing and validation; verified by token and HTTP security tests.
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
@Component
public class TokenService {
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final long expireHours;
    public TokenService(@Value("${app.auth.token-secret}") String secret,
                        @Value("${app.auth.token-expire-hours:12}") long expireHours) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalArgumentException("JWT secret must contain at least 32 bytes");
        var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        var jwtDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("dorm-repair"));
        decoder = jwtDecoder;
        this.expireHours = expireHours;
    }
    public String createToken(CurrentUser user) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("dorm-repair").subject(user.getId().toString())
            .issuedAt(now).expiresAt(now.plusSeconds(expireHours * 3600))
            .claim("username", user.getUsername()).claim("role", user.getRoleCode()).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
    public TokenPayload parse(String token) {
        try {
            var jwt = decoder.decode(token);
            return new TokenPayload(Long.parseLong(jwt.getSubject()), jwt.getClaimAsString("username"),
                jwt.getClaimAsString("role"), jwt.getExpiresAt().toEpochMilli());
        } catch (Exception ex) { throw new BusinessException(401, "登录凭证无效或已过期"); }
    }
}
