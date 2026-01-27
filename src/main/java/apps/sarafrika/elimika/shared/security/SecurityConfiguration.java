package apps.sarafrika.elimika.shared.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(securedEnabled = true)
@Slf4j
public class SecurityConfiguration {

    private final KeyCloakJwtAuthenticationConverter keyCloakJwtAuthenticationConverter;
    private final JwtConfig jwtConfig;
    private final UserSyncFilter userSyncFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security filter chain");

        http
                // Enable CORS (don't disable it since we have a CORS filter)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req ->
                        req.requestMatchers(
                                        "/v2/api-docs",
                                        "/v3/api-docs",
                                        "/v3/api-docs/**",
                                        "/swagger-resources",
                                        "/swagger-resources/**",
                                        "/configuration/ui",
                                        "/configuration/security",
                                        "/swagger-ui/**",
                                        "/webjars/**",
                                        "/swagger-ui.html",
                                        "/actuator/**",
                                        "/health/**",
                                        "/error"
                                )
                                .permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/v1/users", "/api/v1/organisations").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/organisations").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/users/profile-image/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/courses/media/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/assignments/media/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/commerce/catalogue/**").permitAll()
                                .requestMatchers(HttpMethod.OPTIONS).permitAll() // Allow preflight requests
                                .anyRequest()
                                .authenticated()
                )
                .oauth2ResourceServer(auth ->
                        auth.jwt(token -> token
                                .decoder(jwtConfig.jwtDecoder())
                                .jwtAuthenticationConverter(keyCloakJwtAuthenticationConverter)
                        )
                )
                .addFilterAfter(userSyncFilter, BearerTokenAuthenticationFilter.class);

        log.info("Security filter chain configured successfully");
        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        final CorsConfiguration config = new CorsConfiguration();

        // Enhanced CORS configuration
        config.setAllowCredentials(true);

        // Allow multiple origins for different environments
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "https://localhost:*",
                "https://*.sarafrika.com"
        ));

        config.setAllowedHeaders(Arrays.asList(
                HttpHeaders.ORIGIN,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT,
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                "X-Requested-With",
                "X-Auth-Token"
        ));

        config.setAllowedMethods(Arrays.asList(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name(),
                HttpMethod.HEAD.name()
        ));

        // Expose headers that the client might need
        config.setExposedHeaders(Arrays.asList(
                HttpHeaders.AUTHORIZATION,
                "X-Total-Count",
                "X-Page-Number",
                "X-Page-Size"
        ));

        config.setMaxAge(3600L); // Cache preflight response for 1 hour

        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
