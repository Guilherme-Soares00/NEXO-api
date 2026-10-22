package br.com.fiap.nexo;

import br.com.fiap.nexo.alternativa.AlternativaRepository;
import br.com.fiap.nexo.cenario.*;
import br.com.fiap.nexo.mitofato.MitoFatoRepository;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CenarioJpaServiceTest {
    private final CenarioRepository repository = mock(CenarioRepository.class);
    private final CenarioService service = new CenarioService(repository, mock(AlternativaRepository.class),
            mock(MitoFatoRepository.class));

    @Test
    void updatesManagedScenarioWithoutReplacingItsId() {
        var old = new Cenario(new CenarioRequest("Antes", Categoria.SAUDE, "Situação", "Objetivo", "https://example.org/antes"));
        ReflectionTestUtils.setField(old, "id", 7L);
        when(repository.findById(7L)).thenReturn(Optional.of(old));
        when(repository.saveAndFlush(old)).thenReturn(old);
        var request = new CenarioRequest("Depois", Categoria.ENERGIA, "Nova situação", "Novo objetivo", "https://example.org/depois");
        var result = service.atualizar(7L, request);
        verify(repository).saveAndFlush(old);
        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.titulo()).isEqualTo(request.titulo());
        assertThat(result.categoria()).isEqualTo(request.categoria());
        assertThat(result.situacao()).isEqualTo(request.situacao());
        assertThat(result.objetivoEducacional()).isEqualTo(request.objetivoEducacional());
        assertThat(result.fonteUrl()).isEqualTo(request.fonteUrl());
    }

    @Test
    void deletesExistingScenarioAndFlushesConstraintChecks() {
        var item = BeanUtils.instantiateClass(Cenario.class);
        when(repository.findById(7L)).thenReturn(Optional.of(item));
        service.excluir(7L);
        var order = inOrder(repository);
        order.verify(repository).findById(7L);
        order.verify(repository).delete(item);
        order.verify(repository).flush();
    }

    @Test
    void missingScenarioCannotBeUpdatedOrDeleted() {
        var request = new CenarioRequest("Título", Categoria.SAUDE, "Situação", "Objetivo", "https://example.org/fonte");
        assertThatThrownBy(() -> service.atualizar(7L, request)).isInstanceOf(br.com.fiap.nexo.shared.NotFoundException.class);
        assertThatThrownBy(() -> service.excluir(7L)).isInstanceOf(br.com.fiap.nexo.shared.NotFoundException.class);
        verify(repository, never()).saveAndFlush(any());
        verify(repository, never()).delete(any());
    }

    @Test
    void preservesDependencyConflictsWhenDeletingScenario() {
        var item = BeanUtils.instantiateClass(Cenario.class);
        when(repository.findById(7L)).thenReturn(Optional.of(item));
        doThrow(new org.springframework.dao.DataIntegrityViolationException("Dependência")).when(repository).flush();
        assertThatThrownBy(() -> service.excluir(7L)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void createsScenarioThroughJpaUsingTheExistingSequence() throws Exception {
        var request = new CenarioRequest("Título", Categoria.SAUDE, "Situação", "Objetivo", "https://example.org/fonte");

        when(repository.saveAndFlush(any(Cenario.class))).thenAnswer(call -> {
            Cenario item = call.getArgument(0);
            ReflectionTestUtils.setField(item, "id", 7L);
            return item;
        });

        var result = service.criar(request);

        var entity = ArgumentCaptor.forClass(Cenario.class);
        verify(repository).saveAndFlush(entity.capture());
        assertThat(entity.getValue().getTitulo()).isEqualTo(request.titulo());
        assertThat(entity.getValue().getCategoria()).isEqualTo(request.categoria());
        assertThat(entity.getValue().getSituacao()).isEqualTo(request.situacao());
        assertThat(entity.getValue().getObjetivoEducacional()).isEqualTo(request.objetivoEducacional());
        assertThat(entity.getValue().getFonteUrl()).isEqualTo(request.fonteUrl());
        assertThat(result.id()).isEqualTo(7L);
        var id = Cenario.class.getDeclaredField("id");
        assertThat(id.getAnnotation(GeneratedValue.class).strategy()).isEqualTo(GenerationType.SEQUENCE);
        assertThat(id.getAnnotation(SequenceGenerator.class).sequenceName()).isEqualTo("SEQ_NEXO_CENARIO");
        assertThat(id.getAnnotation(SequenceGenerator.class).allocationSize()).isEqualTo(1);
    }
}
