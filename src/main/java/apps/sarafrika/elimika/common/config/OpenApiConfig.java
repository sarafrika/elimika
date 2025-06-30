package apps.sarafrika.elimika.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(
                contact = @Contact(
                        name = "Sarafrika",
                        email = "info@sarafrika.com",
                        url = "https://sarafrika.com"
                ),
                description = "OpenApi documentation for the Elimika Application",
                title = "OpenApi specification - Elimika",
                version = "1.0",
                license = @License(
                        name = "Licence name",
                        url = "https://sarafrika.com"
                ),
                termsOfService = "Terms of service"
        ),
        servers = {
                @Server(
                        description = "Development ENV",
                        url = "https://api.elimika.sarafrika.com"
                )
        },
        security = {
                @SecurityRequirement(name = "bearerAuth"),
                @SecurityRequirement(name = "basicAuth"),
                @SecurityRequirement(name = "oauth2")
        }
)
// Bearer Token Authentication (existing)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT auth description",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
// Basic Authentication (Username/Password)
@SecurityScheme(
        name = "basicAuth",
        description = "Basic Authentication",
        type = SecuritySchemeType.HTTP,
        scheme = "basic"
)
// OAuth2 with Password Flow (Alternative approach)
@SecurityScheme(
        name = "oauth2",
        description = "OAuth2 with password flow",
        type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(
                password = @OAuthFlow(
                        tokenUrl = "/oauth/token",
                        refreshUrl = "/oauth/token"
                )
        )
)
public class OpenApiConfig {
}