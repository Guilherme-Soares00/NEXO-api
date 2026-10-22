package br.com.fiap.nexo;

import br.com.fiap.nexo.alternativa.*;
import br.com.fiap.nexo.cenario.*;
import br.com.fiap.nexo.shared.NotFoundException;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AlternativaJpaServiceTest {
    private final AlternativaRepository repository = mock(AlternativaRepository.class);
    private final CenarioRepository cenarios = mock(CenarioRepository.class);
    private final AlternativaService service = new AlternativaService(repository, cenarios);

    @Test
    void createsAlternativeThroughJpaAndUsesExistingSequence() throws Exception {
        var parent = cenario(1L);
        var request = new AlternativaRequest(1L, "A", "Descrição", "Feedback");
        when(cenarios.findById(1L)).thenReturn(Optional.of(parent));


        when(repository.saveAndFlush(any(Alternativa.class))).thenAnswer(call -> {
            Alternativa item = call.getArgument(0);
            ReflectionTestUtils.setField(item, "id", 7L);
            return item;
        });
        var response = service.criar(request);
        var captor = org.mockito.ArgumentCaptor.forClass(Alternativa.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCenario()).isSameAs(parent);
        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.cenarioId()).isEqualTo(1L);
        assertThat(response.rotulo()).isEqualTo("A");
        assertThat(response.descricao()).isEqualTo("Descrição");
        assertThat(response.feedback()).isEqualTo("Feedback");
        var id = Alternativa.class.getDeclaredField("id");
        assertThat(id.getAnnotation(GeneratedValue.class).strategy()).isEqualTo(GenerationType.SEQUENCE);
        assertThat(id.getAnnotation(SequenceGenerator.class).sequenceName()).isEqualTo("SEQ_NEXO_ALTERNATIVA");
        assertThat(id.getAnnotation(SequenceGenerator.class).allocationSize()).isEqualTo(1);
    }

    @Test
    void updatesAlternativeIncludingItsScenarioWithoutReplacingId() {
        var item = alternativa(cenario(1L));
        var target = cenario(2L);
        when(repository.findById(7L)).thenReturn(Optional.of(item));
        when(cenarios.findById(2L)).thenReturn(Optional.of(target));
        when(repository.saveAndFlush(item)).thenReturn(item);
        var response = service.atualizar(7L, new AlternativaRequest(2L, "B", "Nova descrição", "Novo feedback"));
        verify(repository).saveAndFlush(item);
        assertThat(item.getCenario()).isSameAs(target);
        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.cenarioId()).isEqualTo(2L);
        assertThat(response.rotulo()).isEqualTo("B");
        assertThat(response.descricao()).isEqualTo("Nova descrição");
        assertThat(response.feedback()).isEqualTo("Novo feedback");
    }

    @Test
    void rejectsUnknownScenarioBeforeInserting() {
        assertThatThrownBy(() -> service.criar(new AlternativaRequest(42L, "A", "Descrição", "Feedback")))
                .isInstanceOf(NotFoundException.class).hasMessage("Cenário não encontrado.");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsUnknownScenarioWithoutMutatingExistingAlternative() {
        var parent = cenario(1L);
        var item = alternativa(parent);
        when(repository.findById(7L)).thenReturn(Optional.of(item));
        assertThatThrownBy(() -> service.atualizar(7L, new AlternativaRequest(42L, "B", "Nova", "Novo")))
                .isInstanceOf(NotFoundException.class).hasMessage("Cenário não encontrado.");
        assertThat(item.getCenario()).isSameAs(parent);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsMissingAlternativeOnUpdateAndDelete() {
        assertThatThrownBy(() -> service.atualizar(7L, new AlternativaRequest(1L, "A", "Descrição", "Feedback")))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.excluir(7L)).isInstanceOf(NotFoundException.class);
        verify(repository, never()).saveAndFlush(any());
        verify(repository, never()).delete(any());
    }

    @Test
    void deletesExistingAlternativeAndFlushesConstraints() {
        var item = alternativa(cenario(1L));
        when(repository.findById(7L)).thenReturn(Optional.of(item));
        service.excluir(7L);
        var order = inOrder(repository);
        order.verify(repository).findById(7L);
        order.verify(repository).delete(item);
        order.verify(repository).flush();
    }

    @Test
    void propagatesDuplicateLabelConstraintInsteadOfReturningSuccess() {
        when(cenarios.findById(1L)).thenReturn(Optional.of(cenario(1L)));
        when(repository.saveAndFlush(any(Alternativa.class))).thenThrow(new DataIntegrityViolationException("Rótulo duplicado"));
        assertThatThrownBy(() -> service.criar(new AlternativaRequest(1L, "A", "Descrição", "Feedback")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void propagatesDependentDecisionConstraintOnDelete() {
        var item = alternativa(cenario(1L));
        when(repository.findById(7L)).thenReturn(Optional.of(item));
        doThrow(new DataIntegrityViolationException("Decisão vinculada")).when(repository).flush();
        assertThatThrownBy(() -> service.excluir(7L)).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Cenario cenario(Long id) {
        var item = new Cenario(new CenarioRequest("Cenário", Categoria.PESQUISA, "Situação", "Objetivo", "https://example.org/fonte"));
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private Alternativa alternativa(Cenario parent) {
        var item = BeanUtils.instantiateClass(Alternativa.class);
        ReflectionTestUtils.setField(item, "id", 7L);
        ReflectionTestUtils.setField(item, "cenario", parent);
        ReflectionTestUtils.setField(item, "rotulo", "A");
        return item;
    }
}
