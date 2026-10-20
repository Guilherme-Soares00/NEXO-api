package br.com.fiap.nexo.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    public OpenAPI nexoOpenAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("adminToken", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer")
                        .description("Token administrativo opaco de NEXO_ADMIN_TOKEN, não JWT. Informe somente o token; o Swagger acrescenta Bearer."))).info(new Info()
                .title("NEXO - Laboratório de Decisões")
                .version("1.0.0")
                .description("API educativa do Challenge FIAP ADS 2026, empresa parceira AMAZUL. "
                        + "Gerencia cenários, alternativas e decisões sobre aplicações civis da tecnologia nuclear. "
                        + "As situações e os apelidos de demonstração são fictícios. Não há contas ou autenticação nesta etapa."));
    }
}
