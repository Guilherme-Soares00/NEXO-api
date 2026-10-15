package br.com.fiap.nexo.mitofato;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mito ou Fato pertencente ao cenário, não à alternativa")
public record MitoFatoResponse(
        @Schema(description = "Identificador gerado", example = "1") Long id,
        @Schema(description = "ID do cenário", example = "1") Long cenarioId,
        @Schema(description = "Afirmação educativa") String afirmacao,
        @Schema(description = "Classificação") Classificacao classificacao,
        @Schema(description = "Explicação educativa") String explicacao,
        @Schema(description = "Fonte da afirmação") String fonteUrl) {
    public static MitoFatoResponse from(MitoFato entity) {
        return new MitoFatoResponse(entity.getId(), entity.getCenario().getId(), entity.getAfirmacao(),
                entity.getClassificacao(), entity.getExplicacao(), entity.getFonteUrl());
    }
}
