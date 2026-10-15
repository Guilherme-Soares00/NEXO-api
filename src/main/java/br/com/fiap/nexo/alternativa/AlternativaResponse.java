package br.com.fiap.nexo.alternativa;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representação de alternativa")
public record AlternativaResponse(@Schema(description = "Identificador gerado", example = "1") Long id,
        @Schema(description = "ID do cenário", example = "1") Long cenarioId,
        @Schema(description = "Rótulo") String rotulo,
        @Schema(description = "Descrição") String descricao,
        @Schema(description = "Feedback educacional específico da escolha") String feedback) {
    public static AlternativaResponse from(Alternativa entity) {
        return new AlternativaResponse(entity.getId(), entity.getCenario().getId(), entity.getRotulo(), entity.getDescricao(), entity.getFeedback());
    }
}
