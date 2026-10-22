package br.com.fiap.nexo;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class QueryIntegrationTest extends ApiIntegrationSupport {

    @Test
    void titleSearchIsPartialCaseInsensitiveAndTrimmed() throws Exception {
        long first = createScenario("Hospital e pesquisa clínica");
        long second = createScenario("Decisões no HOSPITAL universitário");
        createScenario("Agricultura e alimentos");
        JsonNode result = ok(get("/cenarios/busca").param("texto", "  ospiTa  "));
        assertPage(result, 0, 10, 2);
        assertThat(ids(result)).containsExactly(first, second);
    }

    @ParameterizedTest
    @ValueSource(strings = {"%", "_", "\\"})
    void titleSearchTreatsSqlPatternCharactersLiterally(String character) throws Exception {
        long matching = createScenario("Pesquisa " + character + " segura");
        createScenario("Pesquisa comum e segura");
        JsonNode result = ok(get("/cenarios/busca").param("texto", character));
        assertPage(result, 0, 10, 1);
        assertThat(ids(result)).containsExactly(matching);
    }

    @Test
    void unmatchedSearchReturnsAnEmptyPage() throws Exception {
        createScenario("Cenário de energia");
        JsonNode result = ok(get("/cenarios/busca").param("texto", "zzzz-nao-encontrado"));
        assertPage(result, 0, 10, 0);
        assertThat(ids(result)).isEmpty();
    }

    @Test
    void scenarioDefaultAndExplicitPaginationAreStableAndOrderedById() throws Exception {
        List<Long> created = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            created.add(createScenario("Página de cenários " + index));
        }
        JsonNode defaults = ok(get("/cenarios"));
        assertPage(defaults, 0, 10, 12);
        assertThat(ids(defaults)).containsExactlyElementsOf(created.subList(0, 10));
        JsonNode second = ok(get("/cenarios").param("page", "1").param("size", "5"));
        assertPage(second, 1, 5, 12);
        assertThat(ids(second)).containsExactlyElementsOf(created.subList(5, 10));
        JsonNode outside = ok(get("/cenarios").param("page", "20").param("size", "5"));
        assertPage(outside, 20, 5, 12);
        assertThat(ids(outside)).isEmpty();
    }

    @Test
    void alternativeAndDecisionListsHaveDefaultPaginationAndAscendingIds() throws Exception {
        long scenarioId = createScenario("Listas relacionadas");
        long first = createAlternative(scenarioId, 2);
        long second = createAlternative(scenarioId, 1);
        JsonNode alternatives = ok(get("/alternativas"));
        assertPage(alternatives, 0, 10, 2);
        assertThat(ids(alternatives)).containsExactly(first, second);
        long decisionA = create("/decisoes", decision(second, "primeiro")).path("id").asLong();
        long decisionB = create("/decisoes", decision(first, "segundo")).path("id").asLong();
        JsonNode decisions = ok(get("/decisoes"));
        assertPage(decisions, 0, 10, 2);
        assertThat(ids(decisions)).containsExactly(decisionA, decisionB);
        JsonNode secondPage = ok(get("/decisoes").param("page", "1").param("size", "1"));
        assertPage(secondPage, 1, 1, 2);
        assertThat(ids(secondPage)).containsExactly(decisionB);
    }

    @Test
    void nestedAlternativesAreFilteredAndOrderedByLabelBeforePagination() throws Exception {
        long scenarioId = createScenario("Cenário com duas alternativas");
        long second = createAlternative(scenarioId, 2);
        long first = createAlternative(scenarioId, 1);
        createAlternative(createScenario("Cenário independente"), 1);
        String path = "/cenarios/" + scenarioId + "/alternativas";
        JsonNode all = ok(get(path));
        assertPage(all, 0, 10, 2);
        assertThat(ids(all)).containsExactly(first, second);
        JsonNode page = ok(get(path).param("page", "1").param("size", "1"));
        assertPage(page, 1, 1, 2);
        assertThat(ids(page)).containsExactly(second);
    }

    @Test
    void searchAppliesPaginationAfterMatching() throws Exception {
        long first = createScenario("Busca paginada A");
        long second = createScenario("Busca paginada B");
        createScenario("Outro título");
        JsonNode result = ok(get("/cenarios/busca").param("texto", "busca paginada")
                .param("page", "1").param("size", "1"));
        assertPage(result, 1, 1, 2);
        assertThat(ids(result)).containsExactly(second).doesNotContain(first);
    }
}
