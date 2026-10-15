package br.com.fiap.nexo.decisao;

import br.com.fiap.nexo.shared.Text;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Campos para criação ou substituição de uma decisão", example = """
        {
          "alternativaId": 1,
          "apelido": "Visitante"
        }
        """)
public record DecisaoRequest(
        @Schema(description = "ID da alternativa existente", example = "1", minimum = "1") @NotNull @Positive Long alternativaId,
        @Schema(description = "Apelido sem identidade verificada", example = "Visitante", minLength = 3, maxLength = 50)
        @NotBlank @Size(min = 3, max = 50) String apelido) {
    public DecisaoRequest { apelido = Text.trim(apelido); }
}
