package br.com.fiap.nexo;

import br.com.fiap.nexo.configuration.SecurityConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

class AdminSecurityConfigurationTest {
    private final WebApplicationContextRunner context = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class, WebMvcAutoConfiguration.class))
            .withUserConfiguration(SecurityConfiguration.class)
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void missingTokenPreventsStartup() {
        context.withPropertyValues("NEXO_ADMIN_TOKEN=").run(application -> {
            assertThat(application).hasFailed();
            assertThat(application.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasRootCauseMessage("Configure NEXO_ADMIN_TOKEN com 64 caracteres hexadecimais aleatórios.");
        });
    }

    @Test
    void malformedTokenPreventsStartup() {
        context.withPropertyValues("NEXO_ADMIN_TOKEN=fraco").run(application -> {
            assertThat(application).hasFailed();
            assertThat(application.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void validConfigurationCreatesTheSecurityChainWithoutExternalIntrospection() {
        context.withPropertyValues(TestCredentials.TOKEN_PROPERTY).run(application -> {
            assertThat(application).hasNotFailed().hasSingleBean(SecurityFilterChain.class);
        });
    }
}
