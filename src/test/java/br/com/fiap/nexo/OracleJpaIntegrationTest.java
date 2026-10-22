package br.com.fiap.nexo;

import br.com.fiap.nexo.cenario.Categoria;
import br.com.fiap.nexo.cenario.CenarioService;
import br.com.fiap.nexo.cenario.CenarioRepository;
import br.com.fiap.nexo.cenario.CenarioRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OracleJpaIntegrationTest extends ApiIntegrationSupport {
    @Autowired
    private CenarioService service;
    @Autowired
    private CenarioRepository cenarios;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private javax.sql.DataSource dataSource;

    @Test
    void successfulApiWriteIsVisibleThroughAnIndependentJdbcConnection() throws Exception {
        long id = createScenario("Gravação confirmada pela API");
        try (var connection = dataSource.getConnection();
             var query = connection.prepareStatement("SELECT TITULO FROM NEXO_CENARIO WHERE ID_CENARIO = ?")) {
            query.setLong(1, id);
            try (var result = query.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("Gravação confirmada pela API");
            }
        }
    }

    @Test
    void nestedCreationPersistsInThePathScenario() throws Exception {
        long scenarioId = createScenario("Cadastro aninhado");
        ObjectNode payload = alternative(scenarioId, 1);
        payload.remove("cenarioId");
        JsonNode created = body(mvc.perform(json(post("/cenarios/" + scenarioId + "/alternativas"), payload))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location")).andReturn());
        assertThat(created.path("cenarioId").asLong()).isEqualTo(scenarioId);
        assertThat(ok(get("/alternativas/" + created.path("id").asLong()))).isEqualTo(created);
        error(json(post("/cenarios/" + Long.MAX_VALUE + "/alternativas"), payload),
                404, "/cenarios/" + Long.MAX_VALUE + "/alternativas");
    }

    @Test
    void jpaWriteRollsBackWithTheSpringTransaction() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Long id = transaction.execute(status -> {
            Long created = service.criar(new CenarioRequest("Rollback Oracle", Categoria.PESQUISA,
                    "Situação de teste", "Verificar atomicidade", "https://example.org/fonte")).id();
            assertThat(cenarios.findById(created)).isPresent();
            status.setRollbackOnly();
            return created;
        });
        assertThat(id).isNotNull();
        assertThat(cenarios.findById(id)).isEmpty();
        assertThat(cenarios.findByTituloContainingIgnoreCase("Rollback Oracle", PageRequest.of(0, 10))).isEmpty();
    }
}
