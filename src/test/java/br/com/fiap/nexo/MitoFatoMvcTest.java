package br.com.fiap.nexo;

import br.com.fiap.nexo.cenario.Cenario;
import br.com.fiap.nexo.cenario.CenarioRepository;
import br.com.fiap.nexo.configuration.JacksonConfiguration;
import br.com.fiap.nexo.mitofato.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MitoFatoController.class)
@Import({MitoFatoService.class, JacksonConfiguration.class, AuthenticatedMvcTestConfiguration.class})
@org.springframework.test.context.TestPropertySource(properties = TestCredentials.TOKEN_PROPERTY)
class MitoFatoMvcTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @MockitoBean
    private MitoFatoRepository repository;

    @MockitoBean
    private CenarioRepository cenarios;

    @Test
    void createsReadsUpdatesListsAndDeletesThroughJpa() throws Exception {
        MitoFato original = item("Afirmação de teste");
        when(cenarios.findById(1L)).thenReturn(Optional.of(original.getCenario()));
        when(repository.saveAndFlush(any(MitoFato.class))).thenAnswer(call -> {
            MitoFato entity = call.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(entity, "id", 10L);
            return entity;
        });
        when(repository.findById(10L)).thenReturn(Optional.of(original));
        mvc.perform(post("/mitos-fatos").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(payload())))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/mitos-fatos/10"))
                .andExpect(jsonPath("$.cenarioId").value(1)).andExpect(jsonPath("$.classificacao").value("FATO"));
        var saved = org.mockito.ArgumentCaptor.forClass(MitoFato.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getAfirmacao()).isEqualTo("Afirmação de teste");
        assertThat(saved.getValue().getExplicacao()).isEqualTo("Explicação de teste");
        assertThat(saved.getValue().getFonteUrl()).isEqualTo("https://example.org/fonte");
        mvc.perform(get("/mitos-fatos/10")).andExpect(status().isOk())
                .andExpect(jsonPath("$.afirmacao").value("Afirmação de teste"));

        mvc.perform(put("/mitos-fatos/10").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(payload().put("afirmacao", "Afirmação atualizada"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.afirmacao").value("Afirmação atualizada"));
        verify(repository).saveAndFlush(original);
        when(repository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(List.of(original)));
        mvc.perform(get("/mitos-fatos")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));
        mvc.perform(delete("/mitos-fatos/10")).andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(repository).delete(original);
        verify(repository).flush();
        when(repository.findById(10L)).thenReturn(Optional.empty());
        mvc.perform(get("/mitos-fatos/10")).andExpect(status().isNotFound());
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"afirmacao", "explicacao", "fonteUrl", "cenarioId", "classificacao"})
    void validatesRequiredFieldsOnPostAndPut(String field) throws Exception {
        ObjectNode body = payload();
        body.remove(field);
        byte[] bytes = mapper.writeValueAsBytes(body);
        mvc.perform(post("/mitos-fatos").contentType(MediaType.APPLICATION_JSON).content(bytes))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/mitos-fatos/10").contentType(MediaType.APPLICATION_JSON).content(bytes))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository, cenarios);
    }

    @ParameterizedTest
    @ValueSource(strings = {"mito", "INCERTO", "", "1"})
    void rejectsUnknownClassification(String value) throws Exception {
        mvc.perform(post("/mitos-fatos").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(payload().put("classificacao", value))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository, cenarios);
    }

    @Test
    void rejectsLongTextInvalidUrlAndUnknownFields() throws Exception {
        for (ObjectNode body : List.of(payload().put("afirmacao", "a".repeat(501)),
                payload().put("explicacao", "a".repeat(1001)), payload().put("fonteUrl", "ftp://example.org"),
                payload().put("afirmacao", 123), payload().put("id", 7), payload().put("cenarioId", 0))) {
            mvc.perform(post("/mitos-fatos").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body)))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(repository, cenarios);
    }

    private ObjectNode payload() {
        return mapper.createObjectNode().put("cenarioId", 1).put("afirmacao", "  Afirmação de teste  ")
                .put("classificacao", "FATO").put("explicacao", "  Explicação de teste  ")
                .put("fonteUrl", "  https://example.org/fonte  ");
    }

    private MitoFato item(String statement) {
        Cenario parent = org.springframework.beans.BeanUtils.instantiateClass(Cenario.class);
        org.springframework.test.util.ReflectionTestUtils.setField(parent, "id", 1L);
        MitoFato item = new MitoFato(parent, new MitoFatoRequest(1L, statement, Classificacao.FATO,
                "Explicação de teste", "https://example.org/fonte"));
        org.springframework.test.util.ReflectionTestUtils.setField(item, "id", 10L);
        return item;
    }
}
