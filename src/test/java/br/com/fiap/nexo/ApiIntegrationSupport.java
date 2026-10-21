package br.com.fiap.nexo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import br.com.fiap.nexo.cenario.CenarioRepository;
import br.com.fiap.nexo.alternativa.AlternativaRepository;
import br.com.fiap.nexo.decisao.DecisaoRepository;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.LinkedHashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.context.annotation.Import(AuthenticatedMvcTestConfiguration.class)
@org.springframework.test.context.TestPropertySource(properties = TestCredentials.TOKEN_PROPERTY)
@ActiveProfiles("test")
abstract class ApiIntegrationSupport {

    private final Map<String, Set<Long>> createdIds = new HashMap<>();

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate cleanupJdbc;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper mapper;

    @Autowired
    private CenarioRepository cenarios;
    @Autowired
    private AlternativaRepository alternativas;
    @Autowired
    private DecisaoRepository decisoes;
    @Autowired
    private br.com.fiap.nexo.mitofato.MitoFatoRepository mitosFatos;

    @BeforeEach
    void requiresEmptyDedicatedSchema() {
        assertThat(cenarios.count()).as("Use um schema Oracle de testes vazio").isZero();
        assertThat(alternativas.count()).isZero();
        assertThat(decisoes.count()).isZero();
        assertThat(mitosFatos.count()).isZero();
    }

    @AfterEach
    void removesOnlyRowsCreatedByThisTest() {
        if (createdIds.isEmpty()) return;
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            removeCreated("decisoes", "NEXO_DECISAO", "ID_DECISAO");
            removeCreated("mitos-fatos", "NEXO_MITO_FATO", "ID_MITO_FATO");
            removeCreated("alternativas", "NEXO_ALTERNATIVA", "ID_ALTERNATIVA");
            removeCreated("cenarios", "NEXO_CENARIO", "ID_CENARIO");
        });
        createdIds.clear();
        assertThat(cenarios.count()).isZero();
        assertThat(alternativas.count()).isZero();
        assertThat(decisoes.count()).isZero();
        assertThat(mitosFatos.count()).isZero();
    }

    private void removeCreated(String resource, String table, String primaryKey) {
        for (Long id : createdIds.getOrDefault(resource, Set.of())) {
            cleanupJdbc.update("DELETE FROM " + table + " WHERE " + primaryKey + " = ?", id);
        }
    }

    protected ObjectNode scenario(String title) {
        return mapper.createObjectNode()
                .put("titulo", title)
                .put("categoria", "SAUDE")
                .put("situacao", "Uma equipe precisa decidir uma aplicação da tecnologia.")
                .put("objetivoEducacional", "Avaliar consequências e benefícios.")
                .put("fonteUrl", "https://example.org/fonte");
    }

    protected ObjectNode alternative(long scenarioId, int order) {
        return mapper.createObjectNode()
                .put("cenarioId", scenarioId)
                .put("rotulo", order == 1 ? "A" : "B")
                .put("descricao", "Descrição da alternativa.")
                .put("feedback", "Consequências da escolha.");
    }

    protected ObjectNode decision(long alternativeId, String nickname) {
        return mapper.createObjectNode()
                .put("alternativaId", alternativeId)
                .put("apelido", nickname);
    }

    protected JsonNode create(String path, ObjectNode payload) throws Exception {
        MvcResult result = mvc.perform(json(post(path), payload))
                .andExpect(status().isCreated()).andReturn();
        JsonNode body = body(result);
        assertThat(body.path("id").isIntegralNumber()).isTrue();
        assertThat(body.path("id").asLong()).isPositive();
        assertThat(result.getResponse().getHeader("Location"))
                .endsWith(path + "/" + body.path("id").asLong());
        return body;
    }

    protected ObjectNode myth(long scenarioId) {
        return mapper.createObjectNode().put("cenarioId", scenarioId)
                .put("afirmacao", "Afirmação de teste").put("classificacao", "FATO")
                .put("explicacao", "Explicação educativa de teste").put("fonteUrl", "https://example.org/fonte");
    }

    protected long createScenario(String title) throws Exception {
        return create("/cenarios", scenario(title)).path("id").asLong();
    }

    protected long createAlternative(long scenarioId, int order) throws Exception {
        return create("/alternativas", alternative(scenarioId, order)).path("id").asLong();
    }

    protected MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request,
                                                  JsonNode payload) throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(payload));
    }

    protected JsonNode body(MvcResult result) throws Exception {
        assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        JsonNode body = mapper.readTree(result.getResponse().getContentAsByteArray());
        if (result.getResponse().getStatus() == 201 && body.path("id").isIntegralNumber()) {
            String path = result.getRequest().getRequestURI();
            String resource = path.substring(path.lastIndexOf('/') + 1);
            assertThat(resource).isIn("cenarios", "alternativas", "decisoes", "mitos-fatos");
            createdIds.computeIfAbsent(resource, key -> new LinkedHashSet<>()).add(body.path("id").asLong());
        }
        return body;
    }

    protected JsonNode ok(MockHttpServletRequestBuilder request) throws Exception {
        return body(mvc.perform(request).andExpect(status().isOk()).andReturn());
    }

    protected JsonNode update(String path, ObjectNode payload) throws Exception {
        return ok(json(put(path), payload));
    }

    protected JsonNode error(MockHttpServletRequestBuilder request, int expectedStatus,
                             String expectedPath) throws Exception {
        MvcResult result = mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn();
        JsonNode body = body(result);
        assertThat(fieldNames(body)).containsExactlyInAnyOrder(
                "timestamp", "status", "error", "message", "path", "errors");
        assertThat(body.path("status").asInt()).isEqualTo(expectedStatus);
        assertThat(body.path("path").asText()).isEqualTo(expectedPath);
        assertThat(body.path("error").asText()).isNotBlank();
        assertThat(body.path("message").asText()).isNotBlank();
        assertThat(Instant.parse(body.path("timestamp").asText())).isNotNull();
        assertThat(body.path("errors").isArray()).isTrue();
        for (JsonNode item : body.path("errors")) {
            assertThat(fieldNames(item)).containsExactlyInAnyOrder("field", "message");
            assertThat(item.path("field").asText()).isNotBlank();
            assertThat(item.path("message").asText()).isNotBlank();
        }
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("org.hibernate", "org.springframework", "java.lang.",
                        "stackTrace", "SQLException", "jdbc:h2", "select ", "insert into ");
        return body;
    }

    protected List<String> fieldNames(JsonNode node) {
        List<String> fields = new ArrayList<>();
        node.fieldNames().forEachRemaining(fields::add);
        return fields;
    }

    protected List<Long> ids(JsonNode page) {
        List<Long> ids = new ArrayList<>();
        page.path("content").forEach(item -> ids.add(item.path("id").asLong()));
        return ids;
    }

    protected void assertPage(JsonNode page, int number, int size, long total) {
        assertThat(fieldNames(page)).containsExactlyInAnyOrder(
                "content", "page", "size", "totalElements", "totalPages");
        assertThat(page.path("content").isArray()).isTrue();
        assertThat(page.path("page").asInt()).isEqualTo(number);
        assertThat(page.path("size").asInt()).isEqualTo(size);
        assertThat(page.path("totalElements").asLong()).isEqualTo(total);
        assertThat(page.path("totalPages").asInt()).isEqualTo((int) Math.ceil((double) total / size));
    }
}
