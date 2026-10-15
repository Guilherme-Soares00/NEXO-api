package br.com.fiap.nexo.decisao;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representação de decisao")
public record DecisaoResponse(@Schema(description = "Identificador gerado", example = "1") Long id,
        @Schema(description = "ID da alternativa", example = "1") Long alternativaId,
        @Schema(description = "Apelido informado") String apelido,
        @Schema(description = "Data e hora local gerada no Oracle, sem fuso ou offset", example = "2026-10-19T12:00:00", type = "string") java.time.LocalDateTime dataEscolha) {
    public static DecisaoResponse from(Decisao entity) {
        return new DecisaoResponse(entity.getId(), entity.getAlternativa().getId(), entity.getApelido(), entity.getDataEscolha());
    }
}
