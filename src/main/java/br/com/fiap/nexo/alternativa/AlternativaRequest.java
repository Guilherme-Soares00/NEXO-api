package br.com.fiap.nexo.alternativa;

import br.com.fiap.nexo.shared.Text;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Campos para criação ou substituição de uma alternativa", example = """
        {
          "cenarioId": 1,
          "rotulo": "A",
          "descricao": "Comparar a primeira aplicação com a alternativa disponível.",
          "feedback": "Feedback específico da escolha com benefícios e limitações."
        }
        """)
public record AlternativaRequest(
        @Schema(description = "ID do cenário existente", example = "1", minimum = "1") @NotNull @Positive Long cenarioId,
        @Schema(description = "Rótulo da alternativa", allowableValues = {"A", "B"}, example = "A")
        @NotBlank @Pattern(regexp = "[AB]", message = "deve ser A ou B") String rotulo,
        @Schema(description = "Descrição da alternativa", minLength = 1, maxLength = 1000) @NotBlank @Size(max = 1000) String descricao,
        @Schema(description = "Feedback específico da escolha; Mito ou Fato pertence ao cenário", minLength = 1, maxLength = 1000)
        @NotBlank @Size(max = 1000) String feedback) {
    public AlternativaRequest {
        rotulo = Text.trim(rotulo);
        descricao = Text.trim(descricao);
        feedback = Text.trim(feedback);
    }


}
