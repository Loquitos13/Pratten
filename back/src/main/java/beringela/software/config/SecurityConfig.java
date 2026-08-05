package beringela.software.config;



import beringela.software.security.JsonAccessDeniedHandler;
import beringela.software.security.JsonAuthenticationEntryPoint;
import beringela.software.security.JwtAuthenticationFilter;
import beringela.software.security.PrincipalRevalidationFilter;
import beringela.software.security.WaiterShiftFilter;

import java.util.Arrays;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;

import org.springframework.core.env.Environment;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.Customizer;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;

import org.springframework.web.cors.CorsConfigurationSource;

import org.springframework.web.cors.UrlBasedCorsConfigurationSource;



@Configuration
@EnableMethodSecurity
public class SecurityConfig {



    private static final String OWNER = "OWNER";

    private static final String MANAGER = "MANAGER";

    private static final String WAITER = "WAITER";

    private static final String KITCHEN = "KITCHEN";

    private static final String SUPERADMIN = "SUPERADMIN";



    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final PrincipalRevalidationFilter principalRevalidationFilter;

    private final WaiterShiftFilter waiterShiftFilter;

    private final JsonAuthenticationEntryPoint authenticationEntryPoint;

    private final JsonAccessDeniedHandler accessDeniedHandler;

    private final Environment environment;

    private final List<String> allowedOrigins;



    public SecurityConfig(

            JwtAuthenticationFilter jwtAuthenticationFilter,

            PrincipalRevalidationFilter principalRevalidationFilter,

            WaiterShiftFilter waiterShiftFilter,

            JsonAuthenticationEntryPoint authenticationEntryPoint,

            JsonAccessDeniedHandler accessDeniedHandler,

            Environment environment,

            @Value("${pratten.security.cors.allowed-origins}") String allowedOrigins) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;

        this.principalRevalidationFilter = principalRevalidationFilter;

        this.waiterShiftFilter = waiterShiftFilter;

        this.authenticationEntryPoint = authenticationEntryPoint;

        this.accessDeniedHandler = accessDeniedHandler;

        this.environment = environment;

        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))

                .map(String::trim)

                .filter(s -> !s.isEmpty())

                .toList();

    }



    @Bean

    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http

                .csrf(AbstractHttpConfigurer::disable)

                .cors(Customizer.withDefaults())

                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                .headers(headers -> {

                    headers.contentTypeOptions(Customizer.withDefaults());

                    headers.referrerPolicy(referrer ->

                            referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));

                    if (isDev()) {

                        headers.frameOptions(frame -> frame.sameOrigin());

                    } else {

                        headers.frameOptions(frame -> frame.deny());

                        headers.httpStrictTransportSecurity(hsts ->

                                hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000));

                    }

                })

                .authorizeHttpRequests(auth -> {

                    auth.requestMatchers("/auth/login", "/auth/register").permitAll()

                            .requestMatchers("/platform/auth/login").permitAll()

                            .requestMatchers("/public/**").permitAll()

                            .requestMatchers("/actuator/health").permitAll();



                    if (isDev()) {

                        auth.requestMatchers("/h2-console/**").permitAll();

                        auth.requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**").permitAll();

                    }



                    auth.requestMatchers("/platform/**").hasRole(SUPERADMIN)

                            .requestMatchers("/reports/**").hasAnyRole(OWNER, MANAGER)

                            .requestMatchers("/staff/**").hasAnyRole(OWNER, MANAGER)

                            .requestMatchers("/tenants/**").hasAnyRole(OWNER, MANAGER)

                            .requestMatchers("/reservations/**").hasAnyRole(OWNER, MANAGER);



                    auth.requestMatchers(HttpMethod.GET, "/categories", "/categories/**")

                            .hasAnyRole(OWNER, MANAGER, WAITER)

                            .requestMatchers(HttpMethod.GET, "/products", "/products/**")

                            .hasAnyRole(OWNER, MANAGER, WAITER)

                            .requestMatchers(HttpMethod.GET, "/menu-items", "/menu-items/**")

                            .hasAnyRole(OWNER, MANAGER, WAITER)

                            .requestMatchers(HttpMethod.POST, "/categories", "/products", "/menu-items")

                            .hasAnyRole(OWNER, MANAGER)

                            .requestMatchers(HttpMethod.PUT, "/categories/*", "/products/*", "/menu-items/*")

                            .hasAnyRole(OWNER, MANAGER)

                            .requestMatchers(HttpMethod.PUT, "/menu-items/*/ingredients")

                            .hasAnyRole(OWNER, MANAGER)

                            .requestMatchers(HttpMethod.PATCH, "/products/*/stock")

                            .hasAnyRole(OWNER, MANAGER)

                            .requestMatchers(HttpMethod.DELETE, "/categories/*", "/products/*", "/menu-items/*")

                            .hasAnyRole(OWNER, MANAGER);



                    auth.requestMatchers(HttpMethod.GET, "/tables", "/tables/**")

                            .hasAnyRole(OWNER, MANAGER, WAITER)

                            .requestMatchers(HttpMethod.POST, "/tables").hasAnyRole(OWNER, MANAGER)

                            .requestMatchers(HttpMethod.PUT, "/tables/*").hasAnyRole(OWNER, MANAGER)

                            .requestMatchers(HttpMethod.DELETE, "/tables/*").hasAnyRole(OWNER, MANAGER)

                            .requestMatchers(HttpMethod.PATCH, "/tables/*/assignment")

                            .hasAnyRole(OWNER, MANAGER)

                            .requestMatchers(HttpMethod.PATCH, "/tables/*/status")

                            .hasAnyRole(OWNER, MANAGER);



                    auth.requestMatchers(HttpMethod.GET, "/orders", "/orders/**")

                            .hasAnyRole(OWNER, MANAGER, WAITER, KITCHEN)

                            .requestMatchers("/orders/**").hasAnyRole(OWNER, MANAGER, WAITER);



                    auth.requestMatchers("/kitchen/**").hasAnyRole(OWNER, MANAGER, KITCHEN);



                    auth.requestMatchers("/shifts/**")

                            .hasAnyRole(OWNER, MANAGER, WAITER, KITCHEN);



                    auth.requestMatchers("/sync/**")

                            .hasAnyRole(OWNER, MANAGER, WAITER, KITCHEN);



                    auth.anyRequest().authenticated();

                })

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .addFilterAfter(principalRevalidationFilter, JwtAuthenticationFilter.class)

                .addFilterAfter(waiterShiftFilter, PrincipalRevalidationFilter.class);



        return http.build();

    }



    /**

     * Impede o utilizador {@code user}/password gerado automaticamente pelo Spring Security.

     */

    @Bean

    public UserDetailsService userDetailsService() {

        return username -> {

            throw new UsernameNotFoundException("Autenticação apenas via JWT");

        };

    }



    @Bean

    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();

    }



    @Bean

    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(allowedOrigins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Tenant-ID"));

        config.setExposedHeaders(List.of("Authorization"));

        config.setAllowCredentials(true);

        config.setMaxAge(3600L);



        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;

    }



    private boolean isDev() {

        return Arrays.asList(environment.getActiveProfiles()).contains("dev")

                || environment.getActiveProfiles().length == 0;

    }

}


