package br.com.fiap.nexo;

import br.com.fiap.nexo.cenario.CenarioRepository;
import br.com.fiap.nexo.mitofato.*;
import br.com.fiap.nexo.shared.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MitoFatoServiceTest {
    @Test
    void nestedListingRequiresExistingScenario() {
        var repository = mock(MitoFatoRepository.class);
        var cenarios = mock(CenarioRepository.class);
        var service = new MitoFatoService(repository, cenarios);
        assertThatThrownBy(() -> service.listarPorCenario(42L, 0, 10))
                .isInstanceOf(NotFoundException.class).hasMessage("Cenário não encontrado.");
        verifyNoInteractions(repository);
    }

    @Test
    void nestedListingFiltersScenarioAndUsesDeterministicPagination() {
        var repository = mock(MitoFatoRepository.class);
        var cenarios = mock(CenarioRepository.class);
        var service = new MitoFatoService(repository, cenarios);
        var page = PageRequest.of(1, 5, Sort.by(Sort.Direction.ASC, "id"));
        when(cenarios.existsById(42L)).thenReturn(true);
        when(repository.findByCenarioId(42L, page)).thenReturn(Page.empty(page));
        var result = service.listarPorCenario(42L, 1, 5);
        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(5);
        verify(repository).findByCenarioId(42L, page);
    }

    @Test
    void nestedListingRejectsInvalidPaginationBeforeQuery() {
        var repository = mock(MitoFatoRepository.class);
        var cenarios = mock(CenarioRepository.class);
        when(cenarios.existsById(42L)).thenReturn(true);
        var service = new MitoFatoService(repository, cenarios);
        assertThatThrownBy(() -> service.listarPorCenario(42L, -1, 10))
                .isInstanceOf(br.com.fiap.nexo.shared.InvalidInputException.class);
        verifyNoInteractions(repository);
    }
}
