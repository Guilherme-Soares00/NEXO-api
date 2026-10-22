package br.com.fiap.nexo;

import br.com.fiap.nexo.alternativa.*;
import br.com.fiap.nexo.cenario.*;
import br.com.fiap.nexo.decisao.*;
import br.com.fiap.nexo.mitofato.*;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {TestCredentials.TOKEN_PROPERTY, "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"})
@AutoConfigureMockMvc
class AdminSecurityTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private CenarioService cenarios;
    @MockitoBean
    private AlternativaService alternativas;
    @MockitoBean
    private DecisaoService decisoes;
    @MockitoBean
    private MitoFatoService mitosFatos;
    @MockitoBean
    private EntityManager entityManager;

    @BeforeEach
    void setup() {
        var cenario = new CenarioResponse(1L, "Cenário", Categoria.SAUDE, "Situação", "Objetivo", "https://example.org");
        when(cenarios.criar(any())).thenReturn(cenario);
        when(cenarios.atualizar(anyLong(), any())).thenReturn(cenario);
        var alternativa = new AlternativaResponse(1L, 1L, "A", "Descrição", "Feedback");
        when(alternativas.criar(any())).thenReturn(alternativa);
        when(alternativas.atualizar(anyLong(), any())).thenReturn(alternativa);
        var mito = new MitoFatoResponse(1L, 1L, "Afirmação", Classificacao.FATO, "Explicação", "https://example.org");
        when(mitosFatos.criar(any())).thenReturn(mito);
        when(mitosFatos.atualizar(anyLong(), any())).thenReturn(mito);
        var decisao = new DecisaoResponse(1L, 1L, "Visitante", LocalDateTime.of(2026, 10, 21, 12, 0));
        when(decisoes.criar(any())).thenReturn(decisao);
        when(decisoes.atualizar(anyLong(), any())).thenReturn(decisao);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/cenarios", "/cenarios/1", "/cenarios/busca?texto=teste", "/cenarios/1/alternativas",
            "/cenarios/1/mitos-fatos", "/cenarios/1/jogo", "/alternativas", "/alternativas/1", "/mitos-fatos", "/mitos-fatos/1"})
    void catalogReadsArePublic(String path) throws Exception {
        mvc.perform(get(path).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    void anonymousPlayerCanRegisterDecisionWithoutCookieOrToken() throws Exception {
        mvc.perform(post("/decisoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alternativaId\":1,\"apelido\":\"Visitante\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Set-Cookie"));
        verify(decisoes).criar(any(DecisaoRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST /cenarios", "PUT /cenarios/1", "DELETE /cenarios/1",
            "POST /alternativas", "POST /cenarios/1/alternativas", "PUT /alternativas/1", "DELETE /alternativas/1",
            "POST /mitos-fatos", "PUT /mitos-fatos/1", "DELETE /mitos-fatos/1",
            "GET /decisoes", "GET /decisoes/1", "PUT /decisoes/1", "DELETE /decisoes/1", "HEAD /decisoes"})
    void allAdministrativeRoutesRequireToken(String target) throws Exception {
        String[] parts = target.split(" ", 2);
        mvc.perform(request(HttpMethod.valueOf(parts[0]), parts[1]).accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer realm=\"nexo-admin\""));
        verifyNoInteractions(cenarios, alternativas, decisoes, mitosFatos);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer invalido", "Bearer", "Basic dGVzdGU6dGVzdGU=", "Bearer token,token"})
    void rejectsInvalidCredentialsWithoutLeakingThem(String authorization) throws Exception {
        String body = mvc.perform(get("/decisoes").header("Authorization", authorization))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/decisoes"))
                .andExpect(jsonPath("$.errors").isEmpty())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain(TestCredentials.TOKEN, authorization, "Exception", "org.springframework");
        verifyNoInteractions(decisoes);
    }

    @Test
    void validBearerCanCreateUpdateAndDeleteCatalogAndManageDecisions() throws Exception {
        for (String base : List.of("/cenarios", "/alternativas", "/mitos-fatos")) {
            String payload = payload(base);
            mvc.perform(post(base).header("Authorization", TestCredentials.AUTHORIZATION)
                            .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isCreated());
            mvc.perform(put(base + "/1").header("Authorization", TestCredentials.AUTHORIZATION)
                            .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk());
            mvc.perform(delete(base + "/1").header("Authorization", TestCredentials.AUTHORIZATION))
                    .andExpect(status().isNoContent());
        }
        mvc.perform(post("/cenarios/1/alternativas").header("Authorization", TestCredentials.AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rotulo\":\"A\",\"descricao\":\"Descrição\",\"feedback\":\"Feedback\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/decisoes").header("Authorization", TestCredentials.AUTHORIZATION)).andExpect(status().isOk());
        mvc.perform(get("/decisoes/1").header("Authorization", TestCredentials.AUTHORIZATION)).andExpect(status().isOk());
        mvc.perform(put("/decisoes/1").header("Authorization", TestCredentials.AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"alternativaId\":1,\"apelido\":\"Visitante\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/decisoes/1").header("Authorization", TestCredentials.AUTHORIZATION)).andExpect(status().isNoContent());
    }

    @Test
    void insufficientAuthorityReturns403InTheApiFormat() throws Exception {
        mvc.perform(delete("/cenarios/1").with(user("leitor").roles("LEITOR")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/cenarios/1"));
        verifyNoInteractions(cenarios);
    }

    @Test
    void doesNotAcceptCredentialsInQueryOrCookie() throws Exception {
        mvc.perform(get("/decisoes").param("access_token", TestCredentials.TOKEN)).andExpect(status().isUnauthorized());
        mvc.perform(get("/decisoes").cookie(new Cookie("Authorization", TestCredentials.AUTHORIZATION)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/cenarios").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("access_token", TestCredentials.TOKEN)).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidBearerDoesNotDowngradeToAnonymousOnPublicRoutes() throws Exception {
        mvc.perform(get("/cenarios").header("Authorization", "Bearer invalido"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/decisoes").header("Authorization", "Bearer invalido")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"alternativaId\":1,\"apelido\":\"Visitante\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(cenarios, decisoes);
    }

    @Test
    void rejectsRepeatedAuthorizationHeaders() throws Exception {
        mvc.perform(get("/decisoes").header("Authorization", TestCredentials.AUTHORIZATION, TestCredentials.AUTHORIZATION))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(decisoes);
    }

    @Test
    void doesNotKeepAuthenticationBetweenRequestsOrCreateSession() throws Exception {
        var result = mvc.perform(get("/decisoes").header("Authorization", TestCredentials.AUTHORIZATION))
                .andExpect(status().isOk()).andExpect(header().doesNotExist("Set-Cookie")).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        mvc.perform(get("/decisoes")).andExpect(status().isUnauthorized());
    }

    @Test
    void documentationRemainsPublicAndUnknownRoutesDoNotBecomePublic() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/nao-existe")).andExpect(status().isUnauthorized());
        mvc.perform(get("/nao-existe").header("Authorization", TestCredentials.AUTHORIZATION)).andExpect(status().isNotFound());
    }

    private String payload(String base) {
        return switch (base) {
            case "/cenarios" -> "{\"titulo\":\"Título\",\"categoria\":\"SAUDE\",\"situacao\":\"Situação\",\"objetivoEducacional\":\"Objetivo\",\"fonteUrl\":\"https://example.org\"}";
            case "/alternativas" -> "{\"cenarioId\":1,\"rotulo\":\"A\",\"descricao\":\"Descrição\",\"feedback\":\"Feedback\"}";
            default -> "{\"cenarioId\":1,\"afirmacao\":\"Afirmação\",\"classificacao\":\"FATO\",\"explicacao\":\"Explicação\",\"fonteUrl\":\"https://example.org\"}";
        };
    }
}
