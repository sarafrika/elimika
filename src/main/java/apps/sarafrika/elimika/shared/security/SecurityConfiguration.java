package apps.sarafrika.elimika.shared.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(securedEnabled = true)
@Slf4j
public class SecurityConfiguration {

    /**
     * Static documentation paths that springdoc does not derive from a configurable property:
     * the Swagger UI's own bundle, the webjars it loads from, and the springfox-era aliases some
     * clients still probe. The specification endpoints themselves are derived from the springdoc
     * properties instead — see {@link #apiDocumentationPaths()}.
     */
    private static final String[] STATIC_API_DOCUMENTATION_PATHS = {
            "/v2/api-docs",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui/**",
            "/webjars/**"
    };

    /**
     * Liveness probes, which must answer before anyone can hold a token: the container
     * healthcheck in {@code docker/compose.yaml} curls {@code /actuator/health} with no
     * credentials, and a load balancer in front of the service does the same. Everything else
     * actuator exposes (info, metrics) is operational detail and is authenticated below.
     */
    private static final String[] HEALTH_PATHS = {
            "/actuator/health",
            "/actuator/health/**",
            "/health/**"
    };

    private final KeyCloakJwtAuthenticationConverter keyCloakJwtAuthenticationConverter;
    private final JwtConfig jwtConfig;
    private final UserSyncFilter userSyncFilter;
    private final ObjectProvider<DomainSecurityService> domainSecurityService;

    /**
     * Whether the API documentation may be read without a token. False unless an environment
     * opts in with {@code APP_API_DOCS_PUBLIC=true} — local development, where browsing
     * {@code /swagger-ui/index.html} without first minting a Keycloak token is the point, and a
     * sandbox or staging box while the frontend client is regenerated from the spec.
     * <p>
     * Where it is false the documentation is not merely "authenticated": anyone can self-register
     * ({@code POST /api/v1/users} is permitAll below), so a signed-in caller is not a
     * confidentiality boundary. It is restricted to platform administrators instead. A browser
     * cannot satisfy that — this service is a bearer-only resource server with no login redirect —
     * so outside an opted-in environment the Swagger UI is unusable by design and the spec is
     * fetched with a platform-admin token (see {@code docs/api-documentation-access.md}).
     */
    @Value("${app.api-docs.public:false}")
    private boolean apiDocsPublic;

    /**
     * Read from the same property springdoc's handler mappings are built from, so the guard cannot
     * drift away from the endpoint it guards. Default matches springdoc's own
     * {@code Constants.DEFAULT_API_DOCS_URL}.
     */
    @Value("${springdoc.api-docs.path:/v3/api-docs}")
    private String apiDocsPath;

    /**
     * Likewise for the Swagger UI entry point, springdoc's
     * {@code Constants.DEFAULT_SWAGGER_UI_PATH}.
     */
    @Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
    private String swaggerUiPath;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security filter chain; API documentation is {}",
                apiDocsPublic
                        ? "served anonymously (app.api-docs.public=true)"
                        : "restricted to platform administrators");

        http
                // Enable CORS (don't disable it since we have a CORS filter)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req -> {
                    req.requestMatchers(HEALTH_PATHS).permitAll()
                            .requestMatchers("/error").permitAll()
                            // Registered ahead of the catch-all, so only health stays anonymous.
                            .requestMatchers("/actuator/**").authenticated();

                    final String[] apiDocumentationPaths = apiDocumentationPaths();
                    if (apiDocsPublic) {
                        req.requestMatchers(apiDocumentationPaths).permitAll();
                    } else {
                        req.requestMatchers(apiDocumentationPaths).access(platformAdministrator());
                    }

                    req.requestMatchers(HttpMethod.POST, "/api/v1/users", "/api/v1/organisations").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/organisations").permitAll()
                            // Credential/profile documents stay authenticated; all other
                            // stored media is public (UUID-named, unguessable keys)
                            .requestMatchers(HttpMethod.GET, "/api/v1/files/profile_documents/**").authenticated()
                            .requestMatchers(HttpMethod.GET, "/api/v1/files/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/users/profile-image/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/courses/media/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/courses/content-media/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/courses/*/reviews").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/assignments/media/**").permitAll()
                            // Handed-in work is not public media: a submitted file is served
                            // only to the learner who uploaded it or the staff who mark its
                            // assignment, which needs an authenticated principal to decide.
                            .requestMatchers(HttpMethod.GET, "/api/v1/assignments/submission-media/**").authenticated()
                            .requestMatchers(HttpMethod.GET, "/api/v1/classes/media/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/certificates/files/**").permitAll()
                            // Certificate verification answers "valid or not" and nothing else.
                            // A stranger holding a printed certificate has no account here, so
                            // this is the one certificate route that must work without a token;
                            // every route that discloses the record itself stays authenticated.
                            .requestMatchers(HttpMethod.GET, "/api/v1/certificates/verify/*").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/commerce/catalogue/**").permitAll()
                            // Whether this environment collects money decides how the cart routes
                            // a learner, which the page must know before anyone signs in.
                            .requestMatchers(HttpMethod.GET, "/api/v1/commerce/payment-mode").permitAll()
                            // Invitation and guardian-consent links must be readable by someone
                            // who has no account yet; acting on them still requires a sign-in.
                            .requestMatchers(HttpMethod.GET, "/api/v1/invitations/token/*").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/guardian-invitations/token/*").permitAll()
                            .requestMatchers(HttpMethod.OPTIONS).permitAll() // Allow preflight requests
                            .anyRequest()
                            .authenticated();
                })
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

    /**
     * Every path that serves the OpenAPI specification or the Swagger UI that renders it. The
     * specification is a complete map of every route, parameter, payload and enum the API accepts,
     * including the internal and administrative surface, so it is treated as internal
     * documentation rather than public content.
     * <p>
     * The specification patterns are derived from {@code springdoc.api-docs.path} rather than
     * written out, because springdoc registers <em>four</em> mappings off that one property and
     * missing any of them leaves the whole spec readable from an alias. Verified against
     * springdoc-openapi 2.8.17:
     * <ul>
     *   <li>{@code OpenApiWebMvcResource} — {@code ${path}} (JSON) and {@code ${path}.yaml};</li>
     *   <li>{@code MultipleOpenApiWebMvcResource} — {@code ${path}/{group}} (JSON) and, note the
     *       ordering, {@code ${path}.yaml/{group}};</li>
     *   <li>{@code SwaggerConfigResource} — {@code ${path}/swagger-config}.</li>
     * </ul>
     * {@code ${path}.yaml} is a suffix on the same path segment, so {@code ${path}/**} does not
     * cover it: it needs its own pattern, and so does the grouped YAML below it.
     * <p>
     * The gate is deliberately <em>not</em> keyed on the active Spring profile:
     * {@code docker/.env.sample} ships {@code SPRING_PROFILES_ACTIVE=dev} and
     * {@code docker/compose.yaml} feeds that whole {@code .env} to the container, so a deployed
     * server may well be running the dev profile and a "dev only" gate would be no gate at all.
     * The single switch is {@code app.api-docs.public} ({@code APP_API_DOCS_PUBLIC}), which
     * defaults to false in every environment and has to be turned on deliberately, per
     * environment.
     */
    private String[] apiDocumentationPaths() {
        final List<String> paths = new ArrayList<>(List.of(
                apiDocsPath,                // JSON specification
                apiDocsPath + "/**",        // grouped JSON specification, /swagger-config
                apiDocsPath + ".yaml",      // YAML specification — same bytes, different encoding
                apiDocsPath + ".yaml/**",   // grouped YAML specification
                swaggerUiPath               // Swagger UI entry point
        ));
        paths.addAll(List.of(STATIC_API_DOCUMENTATION_PATHS));
        return paths.toArray(String[]::new);
    }

    /**
     * Authorises only a platform administrator, i.e. a holder of the {@code admin} domain at the
     * global level rather than inside some organisation. Delegates to
     * {@link DomainSecurityService#isPlatformAdmin()} rather than to a role or authority check,
     * because a domain a user can hold within their own organisation says nothing about whether
     * they may read the platform's internal surface.
     * <p>
     * The service is resolved through an {@link ObjectProvider} on each evaluation: it depends on
     * the tenancy, student and instructor lookup SPIs, and injecting it eagerly would pull that
     * graph into the construction of the filter chain. It fails closed — an anonymous caller has
     * no user UUID, so the decision is false and the entry point answers 401.
     */
    private AuthorizationManager<RequestAuthorizationContext> platformAdministrator() {
        return (authentication, context) ->
                new AuthorizationDecision(domainSecurityService.getObject().isPlatformAdmin());
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
