package br.com.fiap.nexo;

import br.com.fiap.nexo.alternativa.*;
import br.com.fiap.nexo.decisao.*;
import br.com.fiap.nexo.shared.NotFoundException;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import java.time.LocalDateTime;
import java.util.Optional;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DecisaoJpaServiceTest {
    private final DecisaoRepository repository = mock(DecisaoRepository.class);
    private final AlternativaRepository alternativas = mock(AlternativaRepository.class);
    private final DecisaoService service = new DecisaoService(repository, alternativas);
    private final LocalDateTime databaseTime = LocalDateTime.of(2026, 10, 23, 12, 30, 10, 123456000);

    @Test
    void createsDecisionThroughJpaAndReturnsTheDatabaseTimestamp() throws Exception {
        var parent = alternativa(1L);
        var request = new DecisaoRequest(1L, "Visitante");
        when(alternativas.findById(1L)).thenReturn(Optional.of(parent));


        when(repository.saveAndFlush(any(Decisao.class))).thenAnswer(call -> {
            Decisao entity = call.getArgument(0);
            assertThat(entity.getDataEscolha()).isNull();
            ReflectionTestUtils.setField(entity, "id", 7L);
            ReflectionTestUtils.setField(entity, "dataEscolha", databaseTime);
            return entity;
        });
        var result = service.criar(request);
        verify(repository).saveAndFlush(any(Decisao.class));
        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.alternativaId()).isEqualTo(1L);
        assertThat(result.apelido()).isEqualTo("Visitante");
        assertThat(result.dataEscolha()).isEqualTo(databaseTime);
        var id = Decisao.class.getDeclaredField("id");
        assertThat(id.getAnnotation(GeneratedValue.class).strategy()).isEqualTo(GenerationType.SEQUENCE);
        assertThat(id.getAnnotation(SequenceGenerator.class).sequenceName()).isEqualTo("SEQ_NEXO_DECISAO");
        assertThat(id.getAnnotation(SequenceGenerator.class).allocationSize()).isEqualTo(1);
    }

    @Test
    void mapsTimestampAsDatabaseGeneratedOnInsertAndNeverWritable() throws Exception {
        var field = Decisao.class.getDeclaredField("dataEscolha");
        assertThat(field.getAnnotation(Column.class).insertable()).isFalse();
        assertThat(field.getAnnotation(Column.class).updatable()).isFalse();
        assertThat(field.getAnnotation(Generated.class)).isNotNull();
        assertThat(field.getAnnotation(Generated.class).event()).containsExactly(EventType.INSERT);
    }

    @Test
    void updatesNicknameAndAlternativeWithoutChangingIdOrTimestamp() {
        var entity = item(alternativa(1L));
        var target = alternativa(2L);
        when(repository.findById(7L)).thenReturn(Optional.of(entity));
        when(alternativas.findById(2L)).thenReturn(Optional.of(target));
        when(repository.saveAndFlush(entity)).thenReturn(entity);
        var result = service.atualizar(7L, new DecisaoRequest(2L, "Outro apelido"));
        verify(repository).saveAndFlush(entity);
        assertThat(entity.getAlternativa()).isSameAs(target);
        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.alternativaId()).isEqualTo(2L);
        assertThat(result.apelido()).isEqualTo("Outro apelido");
        assertThat(result.dataEscolha()).isEqualTo(databaseTime);
    }

    @Test
    void rejectsUnknownAlternativeBeforeInsertOrUpdate() {
        var parent = alternativa(1L);
        var entity = item(parent);
        when(repository.findById(7L)).thenReturn(Optional.of(entity));
        assertThatThrownBy(() -> service.criar(new DecisaoRequest(42L, "Visitante")))
                .isInstanceOf(NotFoundException.class).hasMessage("Alternativa não encontrada.");
        assertThatThrownBy(() -> service.atualizar(7L, new DecisaoRequest(42L, "Visitante")))
                .isInstanceOf(NotFoundException.class).hasMessage("Alternativa não encontrada.");
        assertThat(entity.getAlternativa()).isSameAs(parent);
        assertThat(entity.getDataEscolha()).isEqualTo(databaseTime);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsMissingDecisionOnUpdateAndDelete() {
        assertThatThrownBy(() -> service.atualizar(7L, new DecisaoRequest(1L, "Visitante"))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.excluir(7L)).isInstanceOf(NotFoundException.class);
        verify(repository, never()).saveAndFlush(any());
        verify(repository, never()).delete(any());
    }

    @Test
    void deletesExistingDecisionAndFlushes() {
        var entity = item(alternativa(1L));
        when(repository.findById(7L)).thenReturn(Optional.of(entity));
        service.excluir(7L);
        var order = inOrder(repository);
        order.verify(repository).findById(7L);
        order.verify(repository).delete(entity);
        order.verify(repository).flush();
    }

    private Alternativa alternativa(Long id) {
        var entity = BeanUtils.instantiateClass(Alternativa.class);
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private Decisao item(Alternativa parent) {
        var entity = BeanUtils.instantiateClass(Decisao.class);
        ReflectionTestUtils.setField(entity, "id", 7L);
        ReflectionTestUtils.setField(entity, "alternativa", parent);
        ReflectionTestUtils.setField(entity, "dataEscolha", databaseTime);
        return entity;
    }
}
