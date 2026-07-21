package beringela.software.security;

import beringela.software.domain.StaffMember;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Issues and validates self-signed HMAC JWTs carrying tenant, user and role. */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(
            @Value("${pratten.security.jwt.secret}") String secret,
            @Value("${pratten.security.jwt.expiration-minutes:480}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public Instant expiresAt() {
        return Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES);
    }

    public String generate(StaffMember user, Instant expiresAt) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("tenantId", user.getTenantId().toString())
                .claim("role", user.getRole().name())
                .claim("name", user.getName())
                .issuedAt(new Date())
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
