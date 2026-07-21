package beringela.software.config;

import beringela.software.security.JwtAuthenticationFilter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private static final String OWNER = "OWNER";
    private static final String MANAGER = "MANAGER";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        // Public / unauthenticated surface.
                        .requestMatchers("/auth/login", "/auth/register").permitAll()
                        .requestMatchers("/public/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // Management-only areas.
                        .requestMatchers("/reports/**").hasAnyRole(OWNER, MANAGER)
                        .requestMatchers("/staff/**").hasAnyRole(OWNER, MANAGER)
                        .requestMatchers("/tenants/**").hasAnyRole(OWNER, MANAGER)

                        // Catalog management (reads stay open to any authenticated staff).
                        .requestMatchers(HttpMethod.POST, "/categories", "/products", "/menu-items")
                        .hasAnyRole(OWNER, MANAGER)
                        .requestMatchers(HttpMethod.PUT, "/categories/*", "/products/*", "/menu-items/*")
                        .hasAnyRole(OWNER, MANAGER)
                        .requestMatchers(HttpMethod.PATCH, "/products/*/stock").hasAnyRole(OWNER, MANAGER)
                        .requestMatchers(HttpMethod.DELETE, "/categories/*", "/products/*", "/menu-items/*")
                        .hasAnyRole(OWNER, MANAGER)

                        // Table lifecycle: creation/edition/assignment is management;
                        // status changes (seat/free) stay open to waiters.
                        .requestMatchers(HttpMethod.POST, "/tables").hasAnyRole(OWNER, MANAGER)
                        .requestMatchers(HttpMethod.PUT, "/tables/*").hasAnyRole(OWNER, MANAGER)
                        .requestMatchers(HttpMethod.DELETE, "/tables/*").hasAnyRole(OWNER, MANAGER)
                        .requestMatchers(HttpMethod.PATCH, "/tables/*/assignment").hasAnyRole(OWNER, MANAGER)

                        // Everything else requires an authenticated staff member.
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Permissive during this phase; restrict allowed origins before production.
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
