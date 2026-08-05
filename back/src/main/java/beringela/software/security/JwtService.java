package beringela.software.security;

import beringela.software.domain.PlatformAdmin;
import beringela.software.domain.StaffMember;
import beringela.software.domain.StaffRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/** Issues and validates self-signed HMAC JWTs carrying tenant, user and role. */
@Service
public class JwtService {

    /** Segredo de desenvolvimento; nunca deve ser usado em produção. */
    static final String DEFAULT_DEV_SECRET =
            "pratten-dev-secret-change-me-please-0123456789abcdef";

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final String issuer;
    private final long expirationMinutes;

    public JwtService(
            @Value("${pratten.security.jwt.secret}") String secret,
            @Value("${pratten.security.jwt.issuer:pratten}") String issuer,
            @Value("${pratten.security.jwt.expiration-minutes:480}") long expirationMinutes,
            Environment environment) {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (prod && DEFAULT_DEV_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "Define PRATTEN_JWT_SECRET em produção (o segredo de desenvolvimento não é seguro).");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "O segredo JWT tem de ter pelo menos " + MIN_SECRET_BYTES + " bytes.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }

    public Instant expiresAt() {
        return Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES);
    }

    public String generate(StaffMember user, Instant expiresAt) {
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .claim("kind", PrincipalKind.STAFF.name())
                .claim("tenantId", user.getTenantId().toString())
                .claim("role", user.getRole().name())
                .claim("name", user.getName())
                .issuedAt(new Date())
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    public String generatePlatform(PlatformAdmin admin, Instant expiresAt) {
        return Jwts.builder()
                .issuer(issuer)
                .subject(admin.getId().toString())
                .claim("kind", PrincipalKind.PLATFORM.name())
                .claim("name", admin.getName())
                .claim("email", admin.getEmail())
                .issuedAt(new Date())
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    /** Token de sessão remota - superadmin actua como OWNER no tenant. */
    public String generateRemoteSession(UUID platformAdminId, String adminName,
            StaffMember actingAs, UUID remoteSessionId, UUID tenantId, Instant expiresAt) {
        return Jwts.builder()
                .issuer(issuer)
                .subject(actingAs.getId().toString())
                .claim("kind", PrincipalKind.REMOTE.name())
                .claim("tenantId", tenantId.toString())
                .claim("role", StaffRole.OWNER.name())
                .claim("name", actingAs.getName())
                .claim("remoteSessionId", remoteSessionId.toString())
                .claim("platformAdminId", platformAdminId.toString())
                .claim("platformAdminName", adminName)
                .issuedAt(new Date())
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .requireIssuer(issuer)
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
