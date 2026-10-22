package br.com.fiap.nexo;

import br.com.fiap.nexo.alternativa.AlternativaRepository;
import br.com.fiap.nexo.cenario.CenarioRepository;
import br.com.fiap.nexo.decisao.DecisaoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration")
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = TestCredentials.TOKEN_PROPERTY)
class OpenApiTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @MockitoBean
    private CenarioRepository cenarios;
    @MockitoBean
    private AlternativaRepository alternativas;
    @MockitoBean
    private DecisaoRepository decisoes;
    @MockitoBean
    private EntityManager entityManager;
    @MockitoBean
    private br.com.fiap.nexo.mitofato.MitoFatoRepository mitosFatos;

    @Test
    void documentsAllCrudRoutesNestedRoutesAndExamples() throws Exception {
        JsonNode document = mapper.readTree(mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
        assertThat(document.path("openapi").asText()).startsWith("3.");
        for (String base : List.of("/cenarios", "/alternativas", "/decisoes", "/mitos-fatos")) {
            assertThat(document.path("paths").path(base).has("get")).isTrue();
            assertWrite(document.path("paths").path(base).path("post"), "201");
            JsonNode item = document.path("paths").path(base + "/{id}");
            assertThat(item.has("get")).isTrue();
            assertWrite(item.path("put"), "200");
            assertThat(item.path("delete").path("responses").has("204")).isTrue();
        }
        JsonNode nested = document.path("paths").path("/cenarios/{id}/alternativas");
        assertThat(nested.has("get")).isTrue();
        assertWrite(nested.path("post"), "201");
        assertThat(document.path("paths").path("/cenarios/{id}/mitos-fatos").has("get")).isTrue();
        assertThat(document.path("paths").path("/cenarios/{id}/jogo").path("get").path("responses").has("409")).isTrue();
        assertThat(document.path("components").path("schemas").path("AlternativaRequest").path("properties").has("ordem")).isFalse();
        assertThat(document.path("components").path("schemas").path("AlternativaResponse").path("properties").has("ordem")).isFalse();
        assertThat(document.path("paths").path("/cenarios/busca").path("get").path("parameters"))
                .anyMatch(parameter -> parameter.path("name").asText().equals("texto")
                        && parameter.path("required").asBoolean());
        for (String schema : List.of("CenarioRequest", "AlternativaRequest", "AlternativaDoCenarioRequest", "DecisaoRequest", "MitoFatoRequest")) {
            assertThat(document.path("components").path("schemas").path(schema).has("example")).isTrue();
        }
        assertThat(document.path("components").path("schemas").path("AlternativaDoCenarioRequest")
                .path("properties").has("cenarioId")).isFalse();
    }

    @Test
    void servesSwaggerUi() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    private void assertWrite(JsonNode operation, String successStatus) {
        assertThat(operation.path("requestBody").path("content").has("application/json")).isTrue();
        for (String code : List.of(successStatus, "400", "404", "409", "500")) {
            assertThat(operation.path("responses").has(code)).as("Resposta HTTP %s", code).isTrue();
        }
    }
    @Test
    void onlyAdministrativeOperationsAdvertiseBearerSecurity() throws Exception {
        JsonNode root = mapper.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray());
        assertThat(root.at("/components/securitySchemes/adminToken/type").asText()).isEqualTo("http");
        assertThat(root.at("/components/securitySchemes/adminToken/scheme").asText()).isEqualTo("bearer");
        assertThat(root.path("security").size()).isZero();
        for (String base : List.of("/cenarios", "/alternativas", "/mitos-fatos")) {
            assertAdministrative(root.path("paths").path(base).path("post"));
            assertAdministrative(root.path("paths").path(base + "/{id}").path("put"));
            assertAdministrative(root.path("paths").path(base + "/{id}").path("delete"));
            assertThat(root.path("paths").path(base).path("get").path("security").size()).isZero();
        }
        assertAdministrative(root.path("paths").path("/cenarios/{id}/alternativas").path("post"));
        assertAdministrative(root.path("paths").path("/decisoes").path("get"));
        for (String method : List.of("get", "put", "delete")) {
            assertAdministrative(root.path("paths").path("/decisoes/{id}").path(method));
        }
        assertThat(root.path("paths").path("/decisoes").path("post").path("security").size()).isZero();
    }

    private void assertAdministrative(JsonNode operation) {
        assertThat(operation.path("security").get(0).has("adminToken")).isTrue();
        assertThat(operation.path("responses").has("401")).isTrue();
        assertThat(operation.path("responses").has("403")).isTrue();
    }

}
