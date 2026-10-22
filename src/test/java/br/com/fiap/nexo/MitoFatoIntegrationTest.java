package br.com.fiap.nexo;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MitoFatoIntegrationTest extends ApiIntegrationSupport {
    @Test
    void createsReadsUpdatesMovesAndDeletesMythThroughApi() throws Exception {
        long first = createScenario("Cenário inicial");
        long second = createScenario("Cenário de destino");
        JsonNode created = create("/mitos-fatos", myth(first));
        String path = "/mitos-fatos/" + created.path("id").asLong();
        assertThat(fieldNames(created)).containsExactlyInAnyOrder("id", "cenarioId", "afirmacao",
                "classificacao", "explicacao", "fonteUrl");
        assertThat(ok(get(path))).isEqualTo(created);
        JsonNode changed = update(path, myth(second).put("classificacao", "MITO").put("afirmacao", "  Outra afirmação  "));
        assertThat(changed.path("cenarioId").asLong()).isEqualTo(second);
        assertThat(changed.path("afirmacao").asText()).isEqualTo("Outra afirmação");
        assertThat(changed.path("classificacao").asText()).isEqualTo("MITO");
        assertThat(ok(get(path))).isEqualTo(changed);
        assertThat(ids(ok(get("/cenarios/" + first + "/mitos-fatos")))).isEmpty();
        assertThat(ids(ok(get("/cenarios/" + second + "/mitos-fatos")))).contains(created.path("id").asLong());
        assertThat(ids(ok(get("/mitos-fatos")))).contains(created.path("id").asLong());
        mvc.perform(delete(path)).andExpect(status().isNoContent());
        error(get(path), 404, path);
    }

    @Test
    void mythDependencyBlocksScenarioDeletionWithoutCascade() throws Exception {
        long id = createScenario("Cenário com descoberta");
        long mythId = create("/mitos-fatos", myth(id)).path("id").asLong();
        error(delete("/cenarios/" + id), 409, "/cenarios/" + id);
        mvc.perform(delete("/mitos-fatos/" + mythId)).andExpect(status().isNoContent());
        mvc.perform(delete("/cenarios/" + id)).andExpect(status().isNoContent());
    }

    @Test
    void missingMythAndMissingParentReturn404() throws Exception {
        long id = createScenario("Cenário existente");
        String missing = "/mitos-fatos/9999999999";
        error(get(missing), 404, missing);
        error(json(put(missing), myth(id)), 404, missing);
        error(delete(missing), 404, missing);
        error(json(post("/mitos-fatos"), myth(9999999999L)), 404, "/mitos-fatos");
        String nested = "/cenarios/9999999999/mitos-fatos";
        error(get(nested), 404, nested);
    }

    @Test
    void playableScenarioRequiresABAndAtLeastOneMyth() throws Exception {
        long id = createScenario("Preparação gradual");
        String path = "/cenarios/" + id + "/jogo";
        error(get(path), 409, path);
        long a = createAlternative(id, 1);
        createAlternative(id, 2);
        error(get(path), 409, path);
        long mythId = create("/mitos-fatos", myth(id)).path("id").asLong();
        JsonNode playable = ok(get(path));
        assertThat(playable.path("alternativas").get(0).path("id").asLong()).isEqualTo(a);
        assertThat(playable.has("mitosFatos")).isFalse();
        create("/decisoes", decision(a, "participante"));
        assertThat(ids(ok(get("/cenarios/" + id + "/mitos-fatos")))).contains(mythId);
    }

    @Test
    void rejectsInvalidClassificationTextLengthsAndClientIds() throws Exception {
        long id = createScenario("Validação da descoberta");
        for (var body : java.util.List.of(myth(id).put("classificacao", "INCERTO"),
                myth(id).put("afirmacao", "a".repeat(501)), myth(id).put("explicacao", "a".repeat(1001)),
                myth(id).put("fonteUrl", "ftp://example.org"), myth(id).put("id", 1))) {
            error(json(post("/mitos-fatos"), body), 400, "/mitos-fatos");
        }
    }
}
