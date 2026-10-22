package br.com.fiap.nexo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class ValidationIntegrationTest extends ApiIntegrationSupport {

    static Stream<Arguments> invalidFields() {
        List<Arguments> cases = new ArrayList<>();
        textCases(cases, "/cenarios", "titulo", 150);
        textCases(cases, "/cenarios", "situacao", 1000);
        textCases(cases, "/cenarios", "objetivoEducacional", 500);
        textCases(cases, "/cenarios", "fonteUrl", 1000);
        textCases(cases, "/alternativas", "rotulo", 1);
        cases.add(Arguments.of("/alternativas", "rotulo", "C"));
        cases.add(Arguments.of("/alternativas", "rotulo", "a"));
        textCases(cases, "/alternativas", "descricao", 1000);
        textCases(cases, "/alternativas", "feedback", 1000);
        textCases(cases, "/decisoes", "apelido", 50);
        cases.add(Arguments.of("/decisoes", "apelido", "ab"));
        cases.add(Arguments.of("/decisoes", "apelido", "  ab  "));
        for (String url : List.of("ftp://example.org/fonte", "javascript:alert(1)", "sem-url", "http://")) {
            cases.add(Arguments.of("/cenarios", "fonteUrl", url));
        }
        for (Object value : new Object[]{null, 0, -1}) {
            cases.add(Arguments.of("/alternativas", "cenarioId", value));
            cases.add(Arguments.of("/decisoes", "alternativaId", value));
        }

        cases.add(Arguments.of("/cenarios", "categoria", (Object) null));
        return cases.stream();
    }

    private static void textCases(List<Arguments> cases, String path, String field, int limit) {
        for (Object value : new Object[]{null, "", "   ", "a".repeat(limit + 1)}) {
            cases.add(Arguments.of(path, field, value));
        }
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void invalidDomainFieldsAreRejectedForCreationAndReplacement(String base, String field,
                                                                 Object value) throws Exception {
        Fixture fixture = fixture(base);
        ObjectNode invalid = fixture.payload().deepCopy();
        invalid.set(field, mapper.valueToTree(value));
        JsonNode createError = error(json(post(base), invalid), 400, base);
        assertThat(createError.path("errors")).anyMatch(item -> item.path("field").asText().equals(field));
        JsonNode updateError = error(json(put(fixture.path()), invalid), 400, fixture.path());
        assertThat(updateError.path("errors")).anyMatch(item -> item.path("field").asText().equals(field));
        assertThat(ok(get(fixture.path())).path("id").asLong()).isPositive();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/cenarios", "/alternativas", "/decisoes"})
    void missingRequiredFieldsAreRejectedOnBothWrites(String base) throws Exception {
        Fixture fixture = fixture(base);
        for (String field : fieldNames(fixture.payload())) {
            ObjectNode invalid = fixture.payload().deepCopy();
            invalid.remove(field);
            error(json(post(base), invalid), 400, base);
            error(json(put(fixture.path()), invalid), 400, fixture.path());
        }
    }

    static Stream<Arguments> invalidTypes() {
        return Stream.of(
                Arguments.of("/cenarios", "categoria", "INVALIDA"),
                Arguments.of("/cenarios", "categoria", "saude"),
                Arguments.of("/cenarios", "titulo", List.of("título")),
                Arguments.of("/alternativas", "cenarioId", "invalido"),
                Arguments.of("/alternativas", "cenarioId", 1.5),
                Arguments.of("/alternativas", "cenarioId", true),
                Arguments.of("/decisoes", "alternativaId", "invalido"),
                Arguments.of("/decisoes", "alternativaId", true));
    }

    @ParameterizedTest
    @MethodSource("invalidTypes")
    void invalidEnumsAndJsonTypesReturn400(String base, String field, Object value) throws Exception {
        Fixture fixture = fixture(base);
        ObjectNode invalid = fixture.payload().deepCopy();
        invalid.set(field, mapper.valueToTree(value));
        error(json(post(base), invalid), 400, base);
        error(json(put(fixture.path()), invalid), 400, fixture.path());
    }

    static Stream<Arguments> scalarTextInputs() {
        return Stream.of("/cenarios:titulo", "/cenarios:situacao", "/cenarios:objetivoEducacional",
                        "/cenarios:fonteUrl", "/alternativas:rotulo", "/alternativas:descricao",
                        "/alternativas:feedback", "/decisoes:apelido")
                .flatMap(input -> {
                    String[] target = input.split(":", 2);
                    return Stream.of(123, 123.5, true)
                            .map(value -> Arguments.of(target[0], target[1], value));
                });
    }

    @ParameterizedTest
    @MethodSource("scalarTextInputs")
    void scalarJsonValuesCannotBeCoercedToText(String base, String field, Object value) throws Exception {
        Fixture fixture = fixture(base);
        ObjectNode invalid = fixture.payload().deepCopy();
        invalid.set(field, mapper.valueToTree(value));
        assertAll(
                () -> error(json(post(base), invalid), 400, base),
                () -> error(json(put(fixture.path()), invalid), 400, fixture.path()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/cenarios", "/alternativas", "/decisoes"})
    void unknownFieldsAndClientControlledIdsOrTimestampsAreRejected(String base) throws Exception {
        Fixture fixture = fixture(base);
        for (String field : List.of("campoDesconhecido", "id", "dataEscolha")) {
            ObjectNode invalid = fixture.payload().deepCopy();
            invalid.put(field, field.equals("dataEscolha") ? "2000-01-01T00:00:00Z" : "123");
            error(json(post(base), invalid), 400, base);
            error(json(put(fixture.path()), invalid), 400, fixture.path());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/cenarios", "/alternativas", "/decisoes"})
    void missingMalformedAndNonObjectJsonBodiesReturn400(String base) throws Exception {
        Fixture fixture = fixture(base);
        for (String content : List.of("", "{", "[]", "null", "\"texto\"", "{}")) {
            error(post(base).contentType(MediaType.APPLICATION_JSON).content(content), 400, base);
            error(put(fixture.path()).contentType(MediaType.APPLICATION_JSON).content(content),
                    400, fixture.path());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "1.5", "9223372036854775808"})
    void invalidPathIdsReturn400ForEveryResource(String id) throws Exception {
        for (String base : List.of("/cenarios", "/alternativas", "/decisoes")) {
            Fixture fixture = fixture(base);
            String path = base + "/" + id;
            error(get(path), 400, path);
            error(json(put(path), fixture.payload()), 400, path);
            error(delete(path), 400, path);
        }
        String nested = "/cenarios/" + id + "/alternativas";
        error(get(nested), 400, nested);
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "page=abc", "page=1.5", "page=2147483648",
            "size=0", "size=-1", "size=51", "size=abc", "size=1.5", "size=2147483648"})
    void invalidPaginationIsRejectedOnEveryListing(String query) throws Exception {
        long scenarioId = createScenario("Paginação válida");
        String[] parameter = query.split("=", 2);
        for (String path : List.of("/cenarios", "/alternativas", "/decisoes", "/cenarios/busca",
                "/cenarios/" + scenarioId + "/alternativas")) {
            error(get(path).param("texto", "Paginação").param(parameter[0], parameter[1]), 400, path);
        }
    }

    @ParameterizedTest
    @CsvSource({"1073741824, 2", "42949673, 50", "2147483647, 50"})
    void paginationOffsetAboveIntegerRangeReturns400(int page, int size) throws Exception {
        assertThat((long) page * size).isGreaterThan(Integer.MAX_VALUE);
        long scenarioId = createScenario("Paginação de fronteira");
        for (String path : listingPaths(scenarioId)) {
            error(get(path).param("texto", "Paginação de fronteira")
                    .param("page", Integer.toString(page)).param("size", Integer.toString(size)), 400, path);
        }
    }

    @ParameterizedTest
    @CsvSource({"2147483647, 1", "1073741823, 2", "42949672, 50"})
    void paginationOffsetWithinIntegerRangeIsAccepted(int page, int size) throws Exception {
        assertThat((long) page * size).isLessThanOrEqualTo(Integer.MAX_VALUE);
        long scenarioId = createScenario("Paginação de fronteira");
        long alternativeId = createAlternative(scenarioId, 1);
        create("/decisoes", decision(alternativeId, "participante"));
        for (String path : listingPaths(scenarioId)) {
            JsonNode result = ok(get(path).param("texto", "Paginação de fronteira")
                    .param("page", Integer.toString(page)).param("size", Integer.toString(size)));
            assertPage(result, page, size, 1);
            assertThat(ids(result)).isEmpty();
        }
    }

    private List<String> listingPaths(long scenarioId) {
        return List.of("/cenarios", "/cenarios/busca", "/cenarios/" + scenarioId + "/alternativas",
                "/alternativas", "/decisoes");
    }

    @Test
    void searchRequiresNonblankTextAtMost150CharactersAfterTrim() throws Exception {
        String path = "/cenarios/busca";
        error(get(path), 400, path);
        for (String text : List.of("", "   ", "a".repeat(151))) {
            error(get(path).param("texto", text), 400, path);
        }
        JsonNode maximum = ok(get(path).param("texto", "  " + "a".repeat(150) + "  "));
        assertThat(maximum.path("content").isArray()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"SAUDE", "AGRICULTURA", "INDUSTRIA", "PESQUISA", "ENERGIA"})
    void everyDocumentedCategoryIsAccepted(String category) throws Exception {
        JsonNode result = create("/cenarios", scenario("Categoria " + category).put("categoria", category));
        assertThat(result.path("categoria").asText()).isEqualTo(category);
    }

    @Test
    void acceptsTrimmedTextAtDocumentedBoundaries() throws Exception {
        ObjectNode scenario = scenario("  " + "t".repeat(150) + "  ")
                .put("situacao", "s".repeat(1000)).put("objetivoEducacional", "o".repeat(500))
                .put("fonteUrl", "https://example.org/" + "a".repeat(980));
        long scenarioId = create("/cenarios", scenario).path("id").asLong();
        ObjectNode alternative = alternative(scenarioId, 1)
                .put("descricao", "d".repeat(1000)).put("feedback", "f".repeat(1000));
        long alternativeId = create("/alternativas", alternative).path("id").asLong();
        create("/decisoes", decision(alternativeId, "  abc  "));
        create("/decisoes", decision(alternativeId, "a".repeat(50)));
        create("/cenarios", scenario("t").put("situacao", "s").put("objetivoEducacional", "o")
                .put("fonteUrl", "http://example.org"));
    }

    private Fixture fixture(String base) throws Exception {
        long scenarioId = createScenario("Base para validação");
        if (base.equals("/cenarios")) {
            return new Fixture(scenario("Cenário válido"), base + "/" + scenarioId);
        }
        long alternativeId = createAlternative(scenarioId, 1);
        if (base.equals("/alternativas")) {
            return new Fixture(alternative(scenarioId, 1), base + "/" + alternativeId);
        }
        ObjectNode payload = decision(alternativeId, "participante");
        long decisionId = create(base, payload).path("id").asLong();
        return new Fixture(payload, base + "/" + decisionId);
    }

    private record Fixture(ObjectNode payload, String path) {
    }
}
