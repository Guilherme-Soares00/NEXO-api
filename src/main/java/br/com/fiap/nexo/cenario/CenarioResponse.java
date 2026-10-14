package br.com.fiap.nexo.cenario;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representação de cenario")
public record CenarioResponse(@Schema(description = "Identificador gerado", example = "1") Long id,
        @Schema(description = "Título") String titulo,
        @Schema(description = "Categoria") Categoria categoria,
        @Schema(description = "Situação") String situacao,
        @Schema(description = "Objetivo educacional") String objetivoEducacional,
        @Schema(description = "URL da fonte") String fonteUrl) {
    public static CenarioResponse from(Cenario entity) {
        return new CenarioResponse(entity.getId(), entity.getTitulo(), entity.getCategoria(), entity.getSituacao(), entity.getObjetivoEducacional(), entity.getFonteUrl());
    }
}
