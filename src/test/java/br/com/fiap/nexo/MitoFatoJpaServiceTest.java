package br.com.fiap.nexo;

import br.com.fiap.nexo.cenario.*;
import br.com.fiap.nexo.mitofato.*;
import br.com.fiap.nexo.shared.NotFoundException;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MitoFatoJpaServiceTest {
    private final MitoFatoRepository repository = mock(MitoFatoRepository.class);
    private final CenarioRepository cenarios = mock(CenarioRepository.class);
    private final MitoFatoService service = new MitoFatoService(repository, cenarios);

    @Test
    void createsMythThroughJpaUsingExistingSequence() throws Exception {
        var parent = cenario(1L);
        var request = request(1L);
        when(cenarios.findById(1L)).thenReturn(Optional.of(parent));


        when(repository.saveAndFlush(any(MitoFato.class))).thenAnswer(call -> {
            MitoFato item = call.getArgument(0);
            ReflectionTestUtils.setField(item, "id", 7L);
            return item;
        });
        var result = service.criar(request);
        verify(repository).saveAndFlush(any(MitoFato.class));
        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.cenarioId()).isEqualTo(1L);
        assertThat(result.afirmacao()).isEqualTo(request.afirmacao());
        assertThat(result.classificacao()).isEqualTo(request.classificacao());
        assertThat(result.explicacao()).isEqualTo(request.explicacao());
        assertThat(result.fonteUrl()).isEqualTo(request.fonteUrl());
        var id = MitoFato.class.getDeclaredField("id");
        assertThat(id.getAnnotation(GeneratedValue.class).strategy()).isEqualTo(GenerationType.SEQUENCE);
        assertThat(id.getAnnotation(SequenceGenerator.class).sequenceName()).isEqualTo("SEQ_NEXO_MITO_FATO");
        assertThat(id.getAnnotation(SequenceGenerator.class).allocationSize()).isEqualTo(1);
    }

    @Test
    void updatesMythIncludingItsParentAndPreservesId() {
        var entity = item(cenario(1L));
        var target = cenario(2L);
        when(repository.findById(7L)).thenReturn(Optional.of(entity));
        when(cenarios.findById(2L)).thenReturn(Optional.of(target));
        when(repository.saveAndFlush(entity)).thenReturn(entity);
        var request = new MitoFatoRequest(2L, "Outra afirmação", Classificacao.MITO, "Outra explicação", "https://example.org/outra");
        var result = service.atualizar(7L, request);
        verify(repository).saveAndFlush(entity);
        assertThat(entity.getCenario()).isSameAs(target);
        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.cenarioId()).isEqualTo(2L);
        assertThat(result.afirmacao()).isEqualTo(request.afirmacao());
        assertThat(result.classificacao()).isEqualTo(request.classificacao());
        assertThat(result.explicacao()).isEqualTo(request.explicacao());
        assertThat(result.fonteUrl()).isEqualTo(request.fonteUrl());
    }

    @Test
    void rejectsUnknownScenarioBeforeInsertOrUpdate() {
        var parent = cenario(1L);
        var entity = item(parent);
        when(repository.findById(7L)).thenReturn(Optional.of(entity));
        assertThatThrownBy(() -> service.criar(request(42L))).isInstanceOf(NotFoundException.class)
                .hasMessage("Cenário não encontrado.");
        assertThatThrownBy(() -> service.atualizar(7L, request(42L))).isInstanceOf(NotFoundException.class)
                .hasMessage("Cenário não encontrado.");
        assertThat(entity.getCenario()).isSameAs(parent);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsMissingMythOnUpdateAndDelete() {
        assertThatThrownBy(() -> service.atualizar(7L, request(1L))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.excluir(7L)).isInstanceOf(NotFoundException.class);
        verify(repository, never()).saveAndFlush(any());
        verify(repository, never()).delete(any());
    }

    @Test
    void deletesExistingMythAndFlushes() {
        var entity = item(cenario(1L));
        when(repository.findById(7L)).thenReturn(Optional.of(entity));
        service.excluir(7L);
        var order = inOrder(repository);
        order.verify(repository).findById(7L);
        order.verify(repository).delete(entity);
        order.verify(repository).flush();
    }

    private MitoFatoRequest request(Long id) {
        return new MitoFatoRequest(id, "Afirmação", Classificacao.FATO, "Explicação", "https://example.org/fonte");
    }

    private Cenario cenario(Long id) {
        var entity = new Cenario(new CenarioRequest("Cenário", Categoria.SAUDE, "Situação", "Objetivo", "https://example.org/fonte"));
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private MitoFato item(Cenario parent) {
        var entity = BeanUtils.instantiateClass(MitoFato.class);
        ReflectionTestUtils.setField(entity, "id", 7L);
        ReflectionTestUtils.setField(entity, "cenario", parent);
        return entity;
    }
}
