package io.github.thibrown1.margem.configuracao;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "jwt";

    @Bean
    public OpenAPI documentacao() {
        return new OpenAPI()
                .info(new Info()
                        .title("Margem API")
                        .version("1.0.0")
                        .description("""
                                Lucro real, pedidos e estoque para quem vende na Shopee.

                                **Como testar:** faça login em `POST /api/auth/login` (demo: `dono` / `dono123`), \
                                copie o token, clique em **Authorize** e cole o token.
                                """)
                        .contact(new Contact().name("Thiago Santos").url("https://github.com/thibrown1"))
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
