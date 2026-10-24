package br.com.fiap.nexo;

import br.com.fiap.nexo.alternativa.AlternativaDoCenarioRequest;
import br.com.fiap.nexo.alternativa.AlternativaRequest;
import br.com.fiap.nexo.cenario.CenarioRequest;
import br.com.fiap.nexo.decisao.DecisaoRequest;
import br.com.fiap.nexo.mitofato.MitoFatoRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostmanSeedContractTest {
    @Test
    void providesValidReusableTestDataWithoutCleanupOrFixedParentIds() throws Exception {
        Path path = Path.of("requests/NEXO.dados-teste.postman_collection.json");
        assertThat(path).isRegularFile();
        var mapper = new ObjectMapper();
        JsonNode collection = mapper.readTree(Files.readString(path));
        List<JsonNode> requests = new ArrayList<>();
        flatten(collection.path("item"), requests);
        assertThat(requests).hasSize(8);
        assertThat(requests.stream().map(item -> item.path("request").path("method").asText()))
                .containsOnly("POST", "GET");
        assertThat(collection.path("auth").toString()).contains("{{adminToken}}");
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            int creates = 0;
            for (var item : requests) {
                var request = item.path("request");
                if (!request.path("method").asText().equals("POST")) continue;
                creates++;
                String url = request.path("url").asText();
                String raw = request.path("body").path("raw").asText();
                Class<?> type;
                if (url.endsWith("/cenarios")) type = CenarioRequest.class;
                else if (url.endsWith("/mitos-fatos")) type = MitoFatoRequest.class;
                else if (url.endsWith("/decisoes")) type = DecisaoRequest.class;
                else if (url.contains("/cenarios/{{scenarioId}}/alternativas")) type = AlternativaDoCenarioRequest.class;
                else type = AlternativaRequest.class;
                assertThat(item.path("event").toString()).contains("pm.collectionVariables.set");
                if (type == AlternativaRequest.class || type == MitoFatoRequest.class) assertThat(raw).contains("{{scenarioId}}");
                if (type == DecisaoRequest.class) {
                    assertThat(raw).contains("{{alternativeId}}");
                    assertThat(request.path("auth").path("type").asText()).isEqualTo("noauth");
                }
                Object dto = mapper.readValue(raw.replace("{{scenarioId}}", "1").replace("{{alternativeId}}", "1"), type);
                assertThat(validator.validate(dto)).isEmpty();
            }
            assertThat(creates).isEqualTo(5);
        }
    }

    @Test
    void providesSpecificEducationalContentWithInstitutionalSources() throws Exception {
        var mapper = new ObjectMapper();
        JsonNode collection = mapper.readTree(Files.readString(Path.of("requests/NEXO.dados-teste.postman_collection.json")));
        List<JsonNode> requests = new ArrayList<>();
        flatten(collection.path("item"), requests);
        for (var item : requests) {
            String raw = item.path("request").path("body").path("raw").asText();
            assertThat(raw).doesNotContain("Exemplo de formato", "módulo DADOS", "serão conferidos");
            String url = item.path("request").path("url").asText();
            if (url.endsWith("/mitos-fatos") && item.path("request").path("method").asText().equals("POST")) {
                JsonNode body = mapper.readTree(raw.replace("{{scenarioId}}", "1"));
                assertThat(body.path("afirmacao").asText()).containsIgnoringCase("radiação").containsIgnoringCase("esterilizar");
                assertThat(body.path("classificacao").asText()).isEqualTo("FATO");
                assertThat(body.path("fonteUrl").asText()).isEqualTo("https://www.iaea.org/topics/medical-sterilization");
            }
        }
    }

    private void flatten(JsonNode items, List<JsonNode> requests) {
        for (var item : items) {
            if (item.has("request")) requests.add(item);
            else flatten(item.path("item"), requests);
        }
    }
}
