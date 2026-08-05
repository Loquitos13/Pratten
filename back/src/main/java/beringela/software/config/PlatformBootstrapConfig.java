package beringela.software.config;

import beringela.software.domain.PlatformAdmin;
import beringela.software.repository.PlatformAdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

/**
 * Cria o primeiro superadmin em produção quando a base está vazia.
 * Em dev o {@link DataSeeder} trata disto.
 */
@Configuration
@Profile("prod")
@ConditionalOnProperty(name = "pratten.platform.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
public class PlatformBootstrapConfig {

    private static final Logger log = LoggerFactory.getLogger(PlatformBootstrapConfig.class);
    private static final int MIN_PASSWORD_LENGTH = 12;

    @Bean
    ApplicationRunner bootstrapPlatformAdmin(PlatformAdminRepository admins,
            PasswordEncoder passwordEncoder,
            @Value("${pratten.platform.admin.email:}") String email,
            @Value("${pratten.platform.admin.password:}") String password,
            @Value("${pratten.platform.admin.name:Platform Admin}") String name) {
        return args -> {
            if (admins.count() > 0) {
                return;
            }
            if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
                throw new IllegalStateException(
                        "Nenhum platform admin existe. Define PRATTEN_PLATFORM_ADMIN_EMAIL "
                                + "e PRATTEN_PLATFORM_ADMIN_PASSWORD antes de arrancar.");
            }
            if (password.length() < MIN_PASSWORD_LENGTH) {
                throw new IllegalStateException(
                        "PRATTEN_PLATFORM_ADMIN_PASSWORD deve ter pelo menos "
                                + MIN_PASSWORD_LENGTH + " caracteres.");
            }
            PlatformAdmin admin = new PlatformAdmin();
            admin.setName(name.trim());
            admin.setEmail(email.trim().toLowerCase());
            admin.setPasswordHash(passwordEncoder.encode(password));
            admins.save(admin);
            log.info("Platform superadmin criado: {} (POST /platform/auth/login)", admin.getEmail());
        };
    }
}
