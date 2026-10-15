package br.com.fiap.nexo.alternativa;

import br.com.fiap.nexo.shared.Text;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Alternativa vinculada ao cenário informado na rota", example = """
        {
          "rotulo": "A",
          "descricao": "Comparar a primeira aplicação com a alternativa disponível.",
          "feedback": "Feedback específico da escolha com benefícios e limitações."
        }
        """)
public record AlternativaDoCenarioRequest(
        @Schema(allowableValues = {"A", "B"}, example = "A")
        @NotBlank @Pattern(regexp = "[AB]", message = "deve ser A ou B") String rotulo,
        @Schema(description = "Descrição da alternativa", example = "Comparar benefícios e limitações da primeira opção.")
        @NotBlank @Size(max = 1000) String descricao,
        @Schema(description = "Benefícios e limitações específicos da escolha", example = "Feedback educativo da opção A.", maxLength = 1000)
        @NotBlank @Size(max = 1000) String feedback) {
    public AlternativaDoCenarioRequest {
        rotulo = Text.trim(rotulo);
        descricao = Text.trim(descricao);
        feedback = Text.trim(feedback);
    }



    public AlternativaRequest paraCenario(Long cenarioId) {
        return new AlternativaRequest(cenarioId, rotulo, descricao, feedback);
    }
}
