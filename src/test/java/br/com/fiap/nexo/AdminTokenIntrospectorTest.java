package br.com.fiap.nexo;

import br.com.fiap.nexo.configuration.AdminTokenIntrospector;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;

import static org.assertj.core.api.Assertions.*;

class AdminTokenIntrospectorTest {
    @Test
    void validTokenGrantsOnlyAdministrativeAuthorityWithoutExposingTheToken() {
        var principal = new AdminTokenIntrospector(TestCredentials.TOKEN).introspect(TestCredentials.TOKEN);
        assertThat(principal.getName()).isEqualTo("administracao");
        assertThat(principal.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
        assertThat(principal.getAttributes()).containsEntry("sub", "administracao").doesNotContainValue(TestCredentials.TOKEN);
    }

    @ParameterizedTest
    @MethodSource("invalidSettings")
    void missingOrMalformedConfigurationFailsClosed(String token) {
        assertThatThrownBy(() -> new AdminTokenIntrospector(token)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Configure NEXO_ADMIN_TOKEN com 64 caracteres hexadecimais aleatórios.");
    }

    @ParameterizedTest
    @MethodSource("invalidCredentials")
    void differentOrMissingCredentialsNeverAuthenticate(String token) {
        var introspector = new AdminTokenIntrospector(TestCredentials.TOKEN);
        assertThatThrownBy(() -> introspector.introspect(token)).isInstanceOf(BadOpaqueTokenException.class)
                .hasMessage("Credencial administrativa inválida.");
    }

    static Stream<String> invalidSettings() {
        return Stream.of(null, "", " ", "fraco", "g".repeat(64), "a".repeat(63), "a".repeat(65), TestCredentials.TOKEN + " ");
    }

    static Stream<String> invalidCredentials() {
        return Stream.of(null, "", "x", "f" + TestCredentials.TOKEN.substring(1),
                TestCredentials.TOKEN.substring(0, 63) + "0", TestCredentials.TOKEN.toUpperCase());
    }
}
