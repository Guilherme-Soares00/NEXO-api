package br.com.fiap.nexo.shared;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Erro padronizado da API")
public record ApiError(
        @Schema(description = "Instante UTC do erro") Instant timestamp,
        @Schema(description = "Código HTTP", example = "400") int status,
        @Schema(description = "Nome do status HTTP", example = "Bad Request") String error,
        @Schema(description = "Mensagem pública do erro") String message,
        @Schema(description = "Caminho solicitado", example = "/cenarios") String path,
        @Schema(description = "Falhas de validação por campo") List<FieldError> errors) {
    @Schema(description = "Falha de validação de um campo")
    public record FieldError(@Schema(description = "Campo inválido") String field,
                             @Schema(description = "Regra violada") String message) {}
}
