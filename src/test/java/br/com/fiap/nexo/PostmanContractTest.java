package br.com.fiap.nexo;

import br.com.fiap.nexo.alternativa.AlternativaDoCenarioRequest;
import br.com.fiap.nexo.alternativa.AlternativaRequest;
import br.com.fiap.nexo.cenario.CenarioRequest;
import br.com.fiap.nexo.decisao.DecisaoRequest;
import br.com.fiap.nexo.mitofato.MitoFatoRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostmanContractTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void allWriteTemplatesMatchCurrentDtosAndValidation() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            for (JsonNode item : requests()) {
                JsonNode request = item.path("request");
                if (!request.path("body").has("raw")) continue;
                String url = request.path("url").asText();
                String raw = request.path("body").path("raw").asText()
                        .replaceAll("\\{\\{[a-zA-Z]+Id\\}\\}", "1");
                Class<?> type = url.contains("/mitos-fatos") ? MitoFatoRequest.class
                        : url.contains("/decisoes") ? DecisaoRequest.class
                        : url.contains("/cenarios/") && url.endsWith("/alternativas") ? AlternativaDoCenarioRequest.class
                        : url.contains("/alternativas") ? AlternativaRequest.class : CenarioRequest.class;
                Object body = mapper.readValue(raw, type);
                var errors = factory.getValidator().validate(body);
                if (item.path("name").asText().equals("Validar erro de campo obrigatório")) {
                    assertThat(errors).isNotEmpty();
                } else {
                    assertThat(errors).as(item.path("name").asText()).isEmpty();
                }
            }
        }
    }

    @Test
    void collectionCoversFourCrudsAndGameDiscoveryRoutes() throws Exception {
        var items = requests();
        for (String base : List.of("/cenarios", "/alternativas", "/decisoes", "/mitos-fatos")) {
            for (String method : List.of("POST", "GET", "PUT", "DELETE")) {
                assertThat(items).as(base + " " + method).anyMatch(item -> {
                    JsonNode request = item.path("request");
                    return request.path("method").asText().equals(method)
                            && request.path("url").asText().startsWith("{{baseUrl}}" + base);
                });
            }
        }
        assertThat(items).anyMatch(item -> item.path("request").path("url").asText().endsWith("/jogo"));
        assertThat(items).anyMatch(item -> item.path("request").path("url").asText()
                .equals("{{baseUrl}}/cenarios/{{scenarioId}}/mitos-fatos"));
    }


    @Test
    void onlyAdministrativeRequestsInheritBearerAndNoSecretIsIncluded() throws Exception {
        JsonNode root = mapper.readTree(Path.of("requests/NEXO.postman_collection.json").toFile());
        assertThat(root.path("auth").path("type").asText()).isEqualTo("bearer");
        assertThat(root.path("auth").path("bearer").get(0).path("value").asText()).isEqualTo("{{adminToken}}");
        assertThat(root.path("variable")).noneMatch(variable -> variable.path("key").asText().equals("adminToken"));
        for (JsonNode item : requests()) {
            JsonNode request = item.path("request");
            String method = request.path("method").asText();
            String url = request.path("url").asText();
            boolean publicRequest = method.equals("GET") && !url.startsWith("{{baseUrl}}/decisoes")
                    || method.equals("POST") && url.equals("{{baseUrl}}/decisoes");
            assertThat(request.path("auth").path("type").asText())
                    .as(item.path("name").asText()).isEqualTo(publicRequest ? "noauth" : "");
        }
    }

    private List<JsonNode> requests() throws Exception {
        List<JsonNode> result = new ArrayList<>();
        collect(mapper.readTree(Path.of("requests/NEXO.postman_collection.json").toFile()).path("item"), result);
        return result;
    }

    private void collect(JsonNode items, List<JsonNode> result) {
        for (JsonNode item : items) {
            if (item.has("request")) result.add(item);
            collect(item.path("item"), result);
        }
    }
}
