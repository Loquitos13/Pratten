package beringela.software.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
public class OpenApiConfig {

    @Bean
    OpenAPI prattenOpenApi(@Value("${server.servlet.context-path:/}") String contextPath) {
        String bearer = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Pratten API")
                        .description("SaaS multi-tenant para restauração. Base path: " + contextPath)
                        .version("0.1")
                        .contact(new Contact().name("Pratten").email("suporte@pratten.pt")))
                .addSecurityItem(new SecurityRequirement().addList(bearer))
                .components(new Components().addSecuritySchemes(bearer,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT staff, platform ou sessão remota")));
    }
}
