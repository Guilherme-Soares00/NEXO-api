package br.com.fiap.nexo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CrudIntegrationTest extends ApiIntegrationSupport {
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void createsReadsUpdatesAndDeletesScenarioWithTrimmedFields() throws Exception {
        ObjectNode payload = scenario("  Hospital de pesquisa  ")
                .put("situacao", "  Situação inicial  ")
                .put("objetivoEducacional", "  Aprender a decidir  ")
                .put("fonteUrl", "  https://example.org/pesquisa  ");
        JsonNode created = create("/cenarios", payload);
        long id = created.path("id").asLong();
        String path = "/cenarios/" + id;
        assertThat(fieldNames(created)).containsExactlyInAnyOrder("id", "titulo", "categoria",
                "situacao", "objetivoEducacional", "fonteUrl");
        assertThat(created.path("titulo").asText()).isEqualTo("Hospital de pesquisa");
        assertThat(created.path("situacao").asText()).isEqualTo("Situação inicial");
        assertThat(created.path("objetivoEducacional").asText()).isEqualTo("Aprender a decidir");
        assertThat(created.path("fonteUrl").asText()).isEqualTo("https://example.org/pesquisa");
        assertThat(ok(get(path))).isEqualTo(created);
        ObjectNode replacement = scenario("Energia renovável").put("categoria", "ENERGIA");
        JsonNode updated = update(path, replacement);
        assertThat(updated.path("id").asLong()).isEqualTo(id);
        assertThat(updated.path("titulo").asText()).isEqualTo("Energia renovável");
        assertThat(updated.path("categoria").asText()).isEqualTo("ENERGIA");
        assertThat(ok(get(path))).isEqualTo(updated);
        mvc.perform(delete(path)).andExpect(status().isNoContent()).andExpect(content().string(""));
        error(get(path), 404, path);
    }

    @Test
    void createsReadsMovesAndDeletesAlternative() throws Exception {
        long firstScenario = createScenario("Primeiro cenário");
        long secondScenario = createScenario("Segundo cenário");
        ObjectNode payload = alternative(firstScenario, 2)
                .put("rotulo", "  B  ")
                .put("descricao", "  Avaliar a aplicação  ")
                .put("feedback", "  Considere o impacto  ");
        JsonNode created = create("/alternativas", payload);
        long id = created.path("id").asLong();
        String path = "/alternativas/" + id;
        assertThat(fieldNames(created)).containsExactlyInAnyOrder("id", "cenarioId", "rotulo",
                "descricao", "feedback");
        assertThat(created.path("cenarioId").asLong()).isEqualTo(firstScenario);
        assertThat(created.path("rotulo").asText()).isEqualTo("B");
        assertThat(created.path("descricao").asText()).isEqualTo("Avaliar a aplicação");
        assertThat(created.path("feedback").asText()).isEqualTo("Considere o impacto");
        assertThat(ok(get(path))).isEqualTo(created);
        JsonNode updated = update(path, alternative(secondScenario, 1));
        assertThat(updated.path("id").asLong()).isEqualTo(id);
        assertThat(updated.path("cenarioId").asLong()).isEqualTo(secondScenario);

        assertThat(updated.path("rotulo").asText()).isEqualTo("A");
        assertThat(ok(get(path))).isEqualTo(updated);
        assertThat(ids(ok(get("/cenarios/" + firstScenario + "/alternativas")))).doesNotContain(id);
        assertThat(ids(ok(get("/cenarios/" + secondScenario + "/alternativas")))).contains(id);
        mvc.perform(delete(path)).andExpect(status().isNoContent()).andExpect(content().string(""));
        error(get(path), 404, path);
    }

    @Test
    void decisionLocalTimestampIsGeneratedByOracleAndPreservedAcrossReplacement() throws Exception {
        long scenarioId = createScenario("Escolhas e consequências");
        long firstAlternative = createAlternative(scenarioId, 1);
        long secondAlternative = createAlternative(scenarioId, 2);
        LocalDateTime before = jdbc.queryForObject("SELECT LOCALTIMESTAMP FROM dual", LocalDateTime.class).minusSeconds(1);
        JsonNode created = create("/decisoes", decision(firstAlternative, "  visitante  "));
        LocalDateTime after = jdbc.queryForObject("SELECT LOCALTIMESTAMP FROM dual", LocalDateTime.class).plusSeconds(1);
        assertThat(fieldNames(created)).containsExactlyInAnyOrder(
                "id", "alternativaId", "apelido", "dataEscolha");
        assertThat(created.path("apelido").asText()).isEqualTo("visitante");
        String timestamp = created.path("dataEscolha").asText();
        assertThat(timestamp).doesNotEndWith("Z");
        assertThat(LocalDateTime.parse(timestamp)).isBetween(before, after);
        String path = "/decisoes/" + created.path("id").asLong();
        assertThat(ok(get(path))).isEqualTo(created);
        JsonNode updated = update(path, decision(secondAlternative, "  novo visitante  "));
        assertThat(updated.path("id")).isEqualTo(created.path("id"));
        assertThat(updated.path("alternativaId").asLong()).isEqualTo(secondAlternative);
        assertThat(updated.path("apelido").asText()).isEqualTo("novo visitante");
        assertThat(updated.path("dataEscolha").asText()).isEqualTo(timestamp);
        assertThat(ok(get(path))).isEqualTo(updated);
        mvc.perform(delete(path)).andExpect(status().isNoContent()).andExpect(content().string(""));
        error(get(path), 404, path);
    }

    @Test
    void rejectsDuplicateLabelOnCreationAndReplacementWithoutChangingExistingRows() throws Exception {
        long scenarioId = createScenario("Ordem única");
        long first = createAlternative(scenarioId, 1);
        long second = createAlternative(scenarioId, 2);
        error(json(post("/alternativas"), alternative(scenarioId, 1)), 409, "/alternativas");
        String secondPath = "/alternativas/" + second;
        error(json(put(secondPath), alternative(scenarioId, 1)), 409, secondPath);
        assertThat(ok(get(secondPath)).path("rotulo").asText()).isEqualTo("B");
        assertThat(ok(get("/alternativas/" + first)).path("rotulo").asText()).isEqualTo("A");
        assertThat(update(secondPath, alternative(scenarioId, 2)).path("id").asLong()).isEqualTo(second);
        createAlternative(createScenario("Outro cenário com ordem própria"), 1);
    }

    @Test
    void rejectsMovingAlternativeIntoOccupiedLabel() throws Exception {
        long source = createScenario("Origem");
        long target = createScenario("Destino");
        long alternativeId = createAlternative(source, 1);
        createAlternative(target, 1);
        String path = "/alternativas/" + alternativeId;
        error(json(put(path), alternative(target, 1)), 409, path);
        assertThat(ok(get(path)).path("cenarioId").asLong()).isEqualTo(source);
    }

    @Test
    void dependenciesBlockDeletionUntilChildrenAreDeleted() throws Exception {
        long scenarioId = createScenario("Integridade das relações");
        long alternativeId = createAlternative(scenarioId, 1);
        long decisionId = create("/decisoes", decision(alternativeId, "participante")).path("id").asLong();
        String scenarioPath = "/cenarios/" + scenarioId;
        String alternativePath = "/alternativas/" + alternativeId;
        error(delete(scenarioPath), 409, scenarioPath);
        error(delete(alternativePath), 409, alternativePath);
        assertThat(ok(get("/decisoes/" + decisionId)).path("alternativaId").asLong()).isEqualTo(alternativeId);
        mvc.perform(delete("/decisoes/" + decisionId)).andExpect(status().isNoContent());
        mvc.perform(delete(alternativePath)).andExpect(status().isNoContent());
        mvc.perform(delete(scenarioPath)).andExpect(status().isNoContent());
    }

    @Test
    void nonexistentParentIsRejectedOnCreationAndReplacement() throws Exception {
        long scenarioId = createScenario("Referências válidas");
        long alternativeId = createAlternative(scenarioId, 1);
        long decisionId = create("/decisoes", decision(alternativeId, "participante")).path("id").asLong();
        error(json(post("/alternativas"), alternative(Long.MAX_VALUE, 1)), 404, "/alternativas");
        error(json(post("/decisoes"), decision(Long.MAX_VALUE, "participante")), 404, "/decisoes");
        String alternativePath = "/alternativas/" + alternativeId;
        String decisionPath = "/decisoes/" + decisionId;
        error(json(put(alternativePath), alternative(Long.MAX_VALUE, 1)), 404, alternativePath);
        error(json(put(decisionPath), decision(Long.MAX_VALUE, "participante")), 404, decisionPath);
        assertThat(ok(get(alternativePath)).path("cenarioId").asLong()).isEqualTo(scenarioId);
        assertThat(ok(get(decisionPath)).path("alternativaId").asLong()).isEqualTo(alternativeId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/cenarios", "/alternativas", "/decisoes"})
    void nonexistentResourceReturns404ForReadUpdateAndDelete(String base) throws Exception {
        long scenarioId = createScenario("Registro para referência");
        long alternativeId = createAlternative(scenarioId, 1);
        ObjectNode payload = switch (base) {
            case "/cenarios" -> scenario("Substituição");
            case "/alternativas" -> alternative(scenarioId, 2);
            default -> decision(alternativeId, "visitante");
        };
        String path = base + "/" + Long.MAX_VALUE;
        error(get(path), 404, path);
        error(json(put(path), payload), 404, path);
        error(delete(path), 404, path);
    }

    @Test
    void nestedListingRequiresExistingScenario() throws Exception {
        String path = "/cenarios/" + Long.MAX_VALUE + "/alternativas";
        error(get(path), 404, path);
    }
}
