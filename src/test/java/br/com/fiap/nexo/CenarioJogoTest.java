package br.com.fiap.nexo;

import br.com.fiap.nexo.alternativa.Alternativa;
import br.com.fiap.nexo.alternativa.AlternativaRepository;
import br.com.fiap.nexo.alternativa.AlternativaService;
import br.com.fiap.nexo.cenario.*;
import br.com.fiap.nexo.configuration.JacksonConfiguration;
import br.com.fiap.nexo.mitofato.MitoFatoRepository;
import br.com.fiap.nexo.mitofato.MitoFatoService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CenarioController.class)
@Import({CenarioService.class, JacksonConfiguration.class, br.com.fiap.nexo.configuration.SecurityConfiguration.class})
@org.springframework.test.context.TestPropertySource(properties = TestCredentials.TOKEN_PROPERTY)
class CenarioJogoTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private CenarioRepository cenarios;
    @MockitoBean
    private AlternativaRepository alternativas;

    @MockitoBean
    private AlternativaService alternativaService;
    @MockitoBean
    private MitoFatoRepository mitosFatos;
    @MockitoBean
    private MitoFatoService mitoFatoService;

    @Test
    void incompleteScenarioIsStillAvailableThroughCrud() throws Exception {
        doReturn(Optional.of(cenario())).when(cenarios).findById(1L);
        mvc.perform(get("/cenarios/1")).andExpect(status().isOk());
    }

    @Test
    void missingScenarioCannotBeOpenedForPlay() throws Exception {
        mvc.perform(get("/cenarios/1/jogo")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "A", "B", "AA", "AB"})
    void incompleteScenarioIsNotExhibitedAsPlayable(String labels) throws Exception {
        doReturn(Optional.of(cenario())).when(cenarios).findById(1L);
        List<Alternativa> items = labels.chars().mapToObj(c -> alternativa(String.valueOf((char) c))).toList();
        when(alternativas.findByCenarioId(any(), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(items));
        when(mitosFatos.existsByCenarioId(1L)).thenReturn(!labels.equals("AB"));
        mvc.perform(get("/cenarios/1/jogo"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("O cenário precisa das alternativas A e B e de pelo menos um Mito ou Fato para exibição no jogo."));
    }

    @Test
    void completeScenarioIsExhibitedWithABWithoutRevealingMythBeforeDecision() throws Exception {
        doReturn(Optional.of(cenario())).when(cenarios).findById(1L);
        doReturn(new org.springframework.data.domain.PageImpl<>(List.of(alternativa("A"), alternativa("B"))))
                .when(alternativas).findByCenarioId(any(), any());
        when(mitosFatos.existsByCenarioId(1L)).thenReturn(true);
        mvc.perform(get("/cenarios/1/jogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cenario.id").value(1))
                .andExpect(jsonPath("$.alternativas[0].rotulo").value("A"))
                .andExpect(jsonPath("$.alternativas[1].rotulo").value("B"))
                .andExpect(jsonPath("$.mitosFatos").doesNotExist());
    }

    @Test
    void nestedMythListingUsesScenarioFromPath() throws Exception {
        when(mitoFatoService.listarPorCenario(42L, 0, 10)).thenReturn(
                new br.com.fiap.nexo.shared.ApiPage<>(List.of(), 0, 10, 0, 0));
        mvc.perform(get("/cenarios/42/mitos-fatos")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        verify(mitoFatoService).listarPorCenario(42L, 0, 10);
    }

    private Cenario cenario() {
        Cenario item = mock(Cenario.class);
        when(item.getId()).thenReturn(1L);
        when(item.getTitulo()).thenReturn("Cenário educativo");
        when(item.getCategoria()).thenReturn(Categoria.PESQUISA);
        return item;
    }

    private Alternativa alternativa(String label) {
        Alternativa item = mock(Alternativa.class);
        when(item.getId()).thenReturn(label.equals("A") ? 1L : 2L);
        doReturn(cenario()).when(item).getCenario();
        when(item.getRotulo()).thenReturn(label);
        return item;
    }
}
