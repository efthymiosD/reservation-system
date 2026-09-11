package com.decoupledx.reservation.shared.adapter.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final List<String> allowedOrigins;

    private final KeycloakLogoutSuccessHandler keycloakLogoutSuccessHandler;

    private final ClientRegistrationRepository clientRegistrationRepository;

    private final AuthenticationFailureHandler failedLoginRedirect = (request, response, exception) ->
            response.sendRedirect("/?loginFailed");

    SecurityConfig(@Value("${app.security.cors.allowed-origins:}") String allowedOrigins,
            ClientRegistrationRepository clientRegistrationRepository,
            KeycloakLogoutSuccessHandler keycloakLogoutSuccessHandler) {
        this.allowedOrigins = allowedOrigins == null || allowedOrigins.isBlank()
                ? List.of()
                : Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
        this.keycloakLogoutSuccessHandler = keycloakLogoutSuccessHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * API: stateless bearer-token resource server, CSRF off. Behavior unchanged
     * for everything under /api/**, the OpenAPI docs and the actuator.
     */
    @Bean
    @Order(1)
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                        "/actuator/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/public/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/health/liveness",
                                "/actuator/health/readiness")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(new JwtRolesConverter())))
                .build();
    }

    /**
     * Browser: session-based OAuth2 login against Keycloak (authorization code) with
     * CSRF protection on. Public venue pages are permitted for GET; the reservation
     * page (/reserve) and personal pages require an authenticated session.
     */
    @Bean
    @Order(2)
    SecurityFilterChain webSecurity(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,
                                "/",
                                "/about",
                                "/opening-hours",
                                "/contact",
                                "/logged-out")
                        .permitAll()
                        .requestMatchers(
                                "/error",
                                "/favicon.ico",
                                "/favicon.svg",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/vendor/**",
                                "/webjars/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                .baseUri("/oauth2/authorization")
                                // Always prompt for credentials at Keycloak: a stale
                                // SSO cookie must never silently re-login a user who
                                // just failed (or gave up on) a previous attempt.
                                .authorizationRequestResolver(new PromptLoginAuthorizationRequestResolver(
                                        clientRegistrationRepository, "/oauth2/authorization")))
                        .failureHandler(failedLoginRedirect))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(keycloakLogoutSuccessHandler))
                .build();
    }
}
