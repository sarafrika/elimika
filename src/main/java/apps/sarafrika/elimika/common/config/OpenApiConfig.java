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
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
// OAuth2 with Password Flow (Keycloak)
@SecurityScheme(
        name = "oauth2",
        description = "OAuth2 with password flow via Keycloak",
        type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(
                password = @OAuthFlow(
                        tokenUrl = "https://signin.sarafrika.com/realms/elimika/protocol/openid-connect/token",
                        refreshUrl = "https://signin.sarafrika.com/realms/elimika/protocol/openid-connect/token"
                )
        )
)
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer phoneNumberSchemaCustomizer() {
        return openApi -> {
            // Add custom schema components for phone numbers
            if (openApi.getComponents() == null) {
                openApi.setComponents(new io.swagger.v3.oas.models.Components());
            }

            // Add reusable phone number schemas for different regions
            addPhoneNumberSchemas(openApi);
        };
    }

    @Bean
    public OpenApiCustomizer urlSchemaCustomizer() {
        return openApi -> {
            // Add custom schema components for URLs
            if (openApi.getComponents() == null) {
                openApi.setComponents(new io.swagger.v3.oas.models.Components());
            }

            // Add reusable URL schemas
            addUrlSchemas(openApi);
        };
    }

    private void addPhoneNumberSchemas(io.swagger.v3.oas.models.OpenAPI openApi) {
        var components = openApi.getComponents();

        // Generic African Phone Number Schema
        Schema<?> africanPhoneSchema = new Schema<String>()
                .type("string")
                .format("phone")
                .pattern("^\\+?[1-9]\\d{7,14}$")
                .example("+254712345678")
                .description("Valid African phone number in international or local format");
        components.addSchemas("AfricanPhoneNumber", africanPhoneSchema);

        // Kenyan Phone Number Schema
        Schema<?> kenyanPhoneSchema = new Schema<String>()
                .type("string")
                .format("phone")
                .pattern("^(\\+254|0)?[17]\\d{8}$")
                .example("+254712345678")
                .description("Valid Kenyan mobile phone number (Safaricom, Airtel formats)");
        components.addSchemas("KenyanPhoneNumber", kenyanPhoneSchema);

        // Nigerian Phone Number Schema
        Schema<?> nigerianPhoneSchema = new Schema<String>()
                .type("string")
                .format("phone")
                .pattern("^(\\+234|0)?[789]\\d{9}$")
                .example("+2348012345678")
                .description("Valid Nigerian mobile phone number (MTN, Glo, Airtel, 9mobile formats)");
        components.addSchemas("NigerianPhoneNumber", nigerianPhoneSchema);

        // East African Phone Number Schema
        Schema<?> eastAfricanPhoneSchema = new Schema<String>()
                .type("string")
                .format("phone")
                .pattern("^(\\+25[0-6]|0)?[1-9]\\d{7,9}$")
                .example("+254712345678")
                .description("Valid East African phone number (Kenya, Uganda, Tanzania, Rwanda, Burundi, South Sudan)");
        components.addSchemas("EastAfricanPhoneNumber", eastAfricanPhoneSchema);

        // West African Phone Number Schema
        Schema<?> westAfricanPhoneSchema = new Schema<String>()
                .type("string")
                .format("phone")
                .pattern("^(\\+2[2-3][0-9]|0)?[1-9]\\d{6,9}$")
                .example("+2348012345678")
                .description("Valid West African phone number (Nigeria, Ghana, Senegal, Mali, etc.)");
        components.addSchemas("WestAfricanPhoneNumber", westAfricanPhoneSchema);

        // Southern African Phone Number Schema
        Schema<?> southernAfricanPhoneSchema = new Schema<String>()
                .type("string")
                .format("phone")
                .pattern("^(\\+2[67]\\d|0)?[1-9]\\d{6,9}$")
                .example("+27821234567")
                .description("Valid Southern African phone number (South Africa, Zimbabwe, Zambia, Botswana, etc.)");
        components.addSchemas("SouthernAfricanPhoneNumber", southernAfricanPhoneSchema);

        // International Phone Number Schema
        Schema<?> internationalPhoneSchema = new Schema<String>()
                .type("string")
                .format("phone")
                .pattern("^\\+?[1-9]\\d{7,14}$")
                .example("+1234567890")
                .description("Valid international phone number in E.164 format");
        components.addSchemas("InternationalPhoneNumber", internationalPhoneSchema);
    }

    private void addUrlSchemas(io.swagger.v3.oas.models.OpenAPI openApi) {
        var components = openApi.getComponents();

        // Generic Valid URL Schema
        Schema<?> validUrlSchema = new Schema<String>()
                .type("string")
                .format("uri")
                .pattern("^https?://[^\\s/$.?#].[^\\s]*$")
                .example("https://example.com")
                .description("A valid URL with HTTP or HTTPS protocol");
        components.addSchemas("ValidUrl", validUrlSchema);

        // Website URL Schema (more permissive)
        Schema<?> websiteUrlSchema = new Schema<String>()
                .type("string")
                .format("uri")
                .pattern("^https?://[^\\s/$.?#].[^\\s]*$")
                .example("https://sarafrika.com")
                .description("A valid website URL for personal or business websites");
        components.addSchemas("WebsiteUrl", websiteUrlSchema);

        // API URL Schema (typically HTTPS)
        Schema<?> apiUrlSchema = new Schema<String>()
                .type("string")
                .format("uri")
                .pattern("^https://[^\\s/$.?#].[^\\s]*$")
                .example("https://api.elimika.sarafrika.com")
                .description("A secure HTTPS URL for API endpoints");
        components.addSchemas("ApiUrl", apiUrlSchema);

        // Image URL Schema
        Schema<?> imageUrlSchema = new Schema<String>()
                .type("string")
                .format("uri")
                .pattern("^https?://[^\\s/$.?#].[^\\s]*\\.(jpg|jpeg|png|gif|webp|svg)$")
                .example("https://images.sarafrika.com/logo.png")
                .description("A valid URL pointing to an image file");
        components.addSchemas("ImageUrl", imageUrlSchema);

        // Document URL Schema
        Schema<?> documentUrlSchema = new Schema<String>()
                .type("string")
                .format("uri")
                .pattern("^https?://[^\\s/$.?#].[^\\s]*\\.(pdf|doc|docx|xls|xlsx|ppt|pptx)$")
                .example("https://docs.sarafrika.com/manual.pdf")
                .description("A valid URL pointing to a document file");
        components.addSchemas("DocumentUrl", documentUrlSchema);

        // Social Media URL Schema
        Schema<?> socialMediaUrlSchema = new Schema<String>()
                .type("string")
                .format("uri")
                .pattern("^https?://(www\\.)?(facebook|twitter|instagram|linkedin|youtube|tiktok)\\.com/[^\\s]*$")
                .example("https://linkedin.com/company/sarafrika")
                .description("A valid social media profile URL");
        components.addSchemas("SocialMediaUrl", socialMediaUrlSchema);
    }
}