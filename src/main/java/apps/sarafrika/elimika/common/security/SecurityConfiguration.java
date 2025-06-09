package apps.sarafrika.elimika.common.security;

import lombok.RequiredArgsConstructor;
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
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    private final KeyCloakJwtAuthenticationConverter keyCloakJwtAuthenticationConverter;
    private final JwtConfig jwtConfig;
    private final UserSyncFilter userSyncFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(AbstractHttpConfigurer::disable)
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
                                        "/health/**"
                                )
                                .permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/v1/users", "/api/v1/organisations").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/organisations").permitAll()
                                .requestMatchers(HttpMethod.OPTIONS).permitAll() // Allow preflight requests
                                .anyRequest()
                                .authenticated() // Changed from permitAll to authenticated for better security
                )
                .oauth2ResourceServer(auth ->
                        auth.jwt(token -> token
                                .decoder(jwtConfig.jwtDecoder())
                                .jwtAuthenticationConverter(keyCloakJwtAuthenticationConverter)
                        )
                )
                // Add the user sync filter after JWT authentication
                .addFilterAfter(userSyncFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsFilter corsFilter() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        final CorsConfiguration config = new CorsConfiguration();

        // Enhanced CORS configuration
        config.setAllowCredentials(true);

        // Allow multiple origins for different environments
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "https://localhost:*",
                "https://*.yourdomain.com" // Replace with your actual domain
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
        return new CorsFilter(source);
    }
}