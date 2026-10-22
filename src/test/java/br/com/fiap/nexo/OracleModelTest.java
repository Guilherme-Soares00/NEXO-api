package br.com.fiap.nexo;

import br.com.fiap.nexo.alternativa.Alternativa;
import br.com.fiap.nexo.alternativa.AlternativaRequest;
import br.com.fiap.nexo.cenario.Categoria;
import br.com.fiap.nexo.cenario.Cenario;
import br.com.fiap.nexo.cenario.CenarioRequest;
import br.com.fiap.nexo.decisao.Decisao;
import br.com.fiap.nexo.decisao.DecisaoRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.validation.Validation;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OracleModelTest {
    @Test
    void mapsOracle19cTablesAndPrimaryKeys() throws Exception {
        assertMapping(Cenario.class, "NEXO_CENARIO", "ID_CENARIO");
        assertMapping(Alternativa.class, "NEXO_ALTERNATIVA", "ID_ALTERNATIVA");
        assertMapping(Decisao.class, "NEXO_DECISAO", "ID_DECISAO");
        assertMapping(br.com.fiap.nexo.mitofato.MitoFato.class, "NEXO_MITO_FATO", "ID_MITO_FATO");
    }

    @Test
    void mapsLengthsAndLocalTimestampWithoutInventingTimezone() throws Exception {
        assertThat(Cenario.class.getDeclaredField("situacao").getAnnotation(Column.class).length()).isEqualTo(1000);
        assertThat(Alternativa.class.getDeclaredField("feedback").getAnnotation(Column.class).length()).isEqualTo(1000);
        assertThat(Decisao.class.getDeclaredField("apelido").getAnnotation(Column.class).length()).isEqualTo(50);
        assertThat(Decisao.class.getDeclaredField("dataEscolha").getType()).isEqualTo(LocalDateTime.class);
        assertThat(Arrays.stream(Alternativa.class.getDeclaredFields()).map(field -> field.getName()))
                .doesNotContain("ordem");
    }

    @Test
    void validatesScenarioAtNewThousandCharacterBoundary() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new CenarioRequest("Título", Categoria.SAUDE, "s".repeat(1000),
                    "Objetivo", "https://example.org/fonte"))).isEmpty();
            assertThat(validator.validate(new CenarioRequest("Título", Categoria.SAUDE, "s".repeat(1001),
                    "Objetivo", "https://example.org/fonte"))).anyMatch(error -> error.getPropertyPath().toString().equals("situacao"));
        }
    }

    @Test
    void acceptsFiftyCharacterNicknameAndKeepsExistingMinimum() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new DecisaoRequest(1L, "a".repeat(50)))).isEmpty();
            assertThat(validator.validate(new DecisaoRequest(1L, "a".repeat(51)))).isNotEmpty();
            assertThat(validator.validate(new DecisaoRequest(1L, "ab"))).isNotEmpty();
        }
    }

    @Test
    void validatesAlternativeFeedbackAtNewBoundaryWithoutOrder() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new AlternativaRequest(1L, "A", "Descrição", "f".repeat(1000)))).isEmpty();
            assertThat(validator.validate(new AlternativaRequest(1L, "A", "Descrição", "f".repeat(1001)))).isNotEmpty();
        }
    }

    private void assertMapping(Class<?> entity, String table, String idColumn) throws Exception {
        assertThat(entity.getAnnotation(Table.class).name()).isEqualTo(table);
        assertThat(entity.getDeclaredField("id").getAnnotation(Column.class).name()).isEqualTo(idColumn);
    }
}
