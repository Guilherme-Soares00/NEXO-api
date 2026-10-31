package br.com.fiap.nexo;

import br.com.fiap.nexo.alternativa.AlternativaController;
import br.com.fiap.nexo.alternativa.AlternativaService;
import br.com.fiap.nexo.alternativa.AlternativaRequest;
import br.com.fiap.nexo.alternativa.AlternativaResponse;
import br.com.fiap.nexo.cenario.CenarioController;
import br.com.fiap.nexo.cenario.CenarioService;
import br.com.fiap.nexo.configuration.JacksonConfiguration;
import br.com.fiap.nexo.decisao.DecisaoController;
import br.com.fiap.nexo.decisao.DecisaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CenarioController.class, AlternativaController.class, DecisaoController.class})
@Import({JacksonConfiguration.class, AuthenticatedMvcTestConfiguration.class})
@org.springframework.test.context.TestPropertySource(properties = TestCredentials.TOKEN_PROPERTY)
class ApiMvcTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @MockitoBean
    private CenarioService cenarios;
    @MockitoBean
    private AlternativaService alternativas;
    @MockitoBean
    private DecisaoService decisoes;
    @MockitoBean
    private br.com.fiap.nexo.mitofato.MitoFatoService mitosFatos;

    @Test
    void createsNestedAlternativeUsingOnlyThePathScenario() throws Exception {
        when(alternativas.criar(any(AlternativaRequest.class))).thenAnswer(invocation -> {
            AlternativaRequest request = invocation.getArgument(0);
            return new AlternativaResponse(11L, request.cenarioId(), request.rotulo(),
                    request.descricao(), request.feedback());
        });
        ObjectNode body = alternative();
        body.remove("cenarioId");
        mvc.perform(post("/cenarios/42/alternativas").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(body)))
                .andExpect(status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Location", "/alternativas/11"))
                .andExpect(jsonPath("$.cenarioId").value(42))
                .andExpect(jsonPath("$.rotulo").value("A"));
    }

    @Test
    void nestedBodyCannotOverridePathScenario() throws Exception {
        mvc.perform(post("/cenarios/42/alternativas").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(alternative())))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(alternativas);
    }

    @ParameterizedTest
    @ValueSource(strings = {"C", "a", "Escolha A", "", " "})
    void rejectsLabelsOutsideAB(String label) throws Exception {
        ObjectNode body = alternative().put("rotulo", label);
        mvc.perform(post("/alternativas").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'rotulo')]").isNotEmpty());
        verifyNoInteractions(alternativas);
    }

    @ParameterizedTest
    @CsvSource({"A, 2", "B, 1"})
    void rejectsRemovedOrderField(String label, int order) throws Exception {
        ObjectNode body = alternative().put("rotulo", label).put("ordem", order);
        mvc.perform(post("/alternativas").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(body)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(alternativas);
    }

    @ParameterizedTest
    @CsvSource({"20001,400", "20002,404", "20003,404", "20004,409", "1,409", "2291,409", "2292,409", "20099,500", "942,500"})
    void translatesOracleCodesWithoutLeakingDetails(int code, int expectedStatus) throws Exception {
        SQLException sql = new SQLException("segredo-interno SQL ORACLE", "99999", code);
        when(cenarios.obter(7L)).thenThrow(new DataIntegrityViolationException("interno", sql));
        String body = mvc.perform(get("/cenarios/7"))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.path").value("/cenarios/7"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("segredo-interno", "SQL ORACLE", "SQLException");
    }

    @Test
    void findsOracleErrorInNextSqlException() throws Exception {
        SQLException wrapper = new SQLException("wrapper");
        wrapper.setNextException(new SQLException("interno", "99999", 20002));
        when(cenarios.obter(7L)).thenThrow(new IllegalStateException(wrapper));
        mvc.perform(get("/cenarios/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Registro não encontrado."));
    }

    @Test
    void validationMessagesArePortugueseEvenWithEnglishLocale() throws Exception {
        mvc.perform(post("/decisoes").header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alternativaId\":1,\"apelido\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.message == 'não deve estar em branco')]").isNotEmpty());
        verifyNoInteractions(decisoes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{\"alternativaId\":1,\"apelido\":\"visitante\",\"id\":10}",
            "{\"alternativaId\":1,\"apelido\":123}", "{\"alternativaId\":1.5,\"apelido\":\"visitante\"}"})
    void rejectsMalformedOrUnexpectedJson(String body) throws Exception {
        mvc.perform(post("/decisoes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(decisoes);
    }

    @Test
    void serializesDecisionTimestampWithoutUtcOrOffset() throws Exception {
        when(decisoes.obter(1L)).thenReturn(new br.com.fiap.nexo.decisao.DecisaoResponse(1L, 2L,
                "participante", java.time.LocalDateTime.of(2026, 10, 21, 14, 30, 0, 123456000)));
        mvc.perform(get("/decisoes/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.dataEscolha").value("2026-10-21T14:30:00.123456"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"apelido\":\"Primeiro\",\"apelido\":\"Segundo\",\"alternativaId\":1}",
            "{\"alternativaId\":1,\"apelido\":\"Primeiro\",\"apelido\":\"Segundo\"}"})
    void rejectsDuplicateJsonKeysRegardlessOfFieldOrder(String body) throws Exception {
        mvc.perform(post("/decisoes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(decisoes);
    }

    @Test
    void rejectsOversizedStringsDuringParsingBeforeValidation() {
        assertThatThrownBy(() -> mapper.readTree("{\"texto\":\"" + "a".repeat(10000) + "\"}"))
                .isInstanceOf(com.fasterxml.jackson.core.exc.StreamConstraintsException.class);
    }

    @Test
    void rejectsExcessiveJsonDepthDuringParsing() {
        assertThatThrownBy(() -> mapper.readTree("[".repeat(30) + "0" + "]".repeat(30)))
                .isInstanceOf(com.fasterxml.jackson.core.exc.StreamConstraintsException.class);
    }

    @Test
    void rejectsOversizedNumbersDuringParsing() {
        assertThatThrownBy(() -> mapper.readTree("9".repeat(100)))
                .isInstanceOf(com.fasterxml.jackson.core.exc.StreamConstraintsException.class);
    }

    @Test
    void oversizedHttpInputReturns400WithoutCallingService() throws Exception {
        mvc.perform(post("/decisoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alternativaId\":1,\"apelido\":\"" + "a".repeat(10000) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isEmpty())
                .andExpect(jsonPath("$.message").value("Entrada inválida. Verifique campos, tipos e parâmetros obrigatórios."));
        verifyNoInteractions(decisoes);
    }

    @Test
    void methodNotAllowedPreservesAllowHeader() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/cenarios"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Allow", org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("GET"),
                                org.hamcrest.Matchers.containsString("POST"))));
    }

    private ObjectNode alternative() {
        return mapper.createObjectNode().put("cenarioId", 1).put("rotulo", "A")
                .put("descricao", "Alternativa educativa.").put("feedback", "Benefícios e limitações.");
    }
}
