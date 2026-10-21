package br.com.fiap.nexo;

import br.com.fiap.nexo.configuration.SecurityConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@TestConfiguration(proxyBeanMethods = false)
@Import(SecurityConfiguration.class)
class AuthenticatedMvcTestConfiguration {
    @Bean
    MockMvcBuilderCustomizer administrativeRequests() {
        return builder -> builder.defaultRequest(get("/").header("Authorization", TestCredentials.AUTHORIZATION));
    }
}
