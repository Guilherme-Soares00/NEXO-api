package br.com.fiap.nexo.configuration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

public class AdminTokenIntrospector implements OpaqueTokenIntrospector {
    private final byte[] expectedToken;

    public AdminTokenIntrospector(String token) {
        if (token == null || !token.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("Configure NEXO_ADMIN_TOKEN com 64 caracteres hexadecimais aleatórios.");
        }
        expectedToken = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        if (token == null || !MessageDigest.isEqual(expectedToken, token.getBytes(StandardCharsets.UTF_8))) {
            throw new BadOpaqueTokenException("Credencial administrativa inválida.");
        }
        return new DefaultOAuth2AuthenticatedPrincipal("administracao", Map.of("sub", "administracao"),
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
