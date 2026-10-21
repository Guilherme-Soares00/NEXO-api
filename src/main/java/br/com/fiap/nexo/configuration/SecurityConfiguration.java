package br.com.fiap.nexo.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    private static final String[] PUBLIC_READ_PATHS = {
            "/cenarios", "/cenarios/{id}", "/cenarios/busca", "/cenarios/{id}/alternativas",
            "/cenarios/{id}/mitos-fatos", "/cenarios/{id}/jogo", "/alternativas", "/alternativas/{id}",
            "/mitos-fatos", "/mitos-fatos/{id}", "/swagger-ui.html", "/swagger-ui/**",
            "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml"
    };

    @Bean
    public OpaqueTokenIntrospector adminTokenIntrospector(@Value("${NEXO_ADMIN_TOKEN:}") String token) {
        return new AdminTokenIntrospector(token);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, OpaqueTokenIntrospector introspector,
                                                  ObjectMapper mapper) throws Exception {
        var errors = new ApiSecurityHandler(mapper);
        var resolver = new DefaultBearerTokenResolver();
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_READ_PATHS).permitAll()
                        .requestMatchers(HttpMethod.HEAD, PUBLIC_READ_PATHS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/decisoes").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .exceptionHandling(handler -> handler.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .oauth2ResourceServer(oauth -> oauth
                        .bearerTokenResolver(request -> {
                            if (Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION)).size() > 1) {
                                throw new OAuth2AuthenticationException(new OAuth2Error("invalid_request"));
                            }
                            return resolver.resolve(request);
                        })
                        .opaqueToken(opaque -> opaque.introspector(introspector))
                        .authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .build();
    }
}
