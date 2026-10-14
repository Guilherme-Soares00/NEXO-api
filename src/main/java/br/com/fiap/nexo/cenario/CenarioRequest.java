package br.com.fiap.nexo.cenario;

import br.com.fiap.nexo.shared.HttpUrl;
import br.com.fiap.nexo.shared.Text;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Campos para criação ou substituição de um cenário", example = """
        {
          "titulo": "Tecnologia nuclear no cotidiano",
          "categoria": "PESQUISA",
          "situacao": "Uma turma compara duas aplicações civis da tecnologia nuclear.",
          "objetivoEducacional": "Identificar benefícios e limitações das alternativas.",
          "fonteUrl": "https://www.gov.br/amazul"
        }
        """)
public record CenarioRequest(
        @Schema(description = "Título do cenário", example = "Radioisótopos no hospital", minLength = 1, maxLength = 150)
        @NotBlank @Size(max = 150) String titulo,
        @Schema(description = "Categoria de aplicação", example = "SAUDE") @NotNull Categoria categoria,
        @Schema(description = "Situação a ser analisada", minLength = 1, maxLength = 1000)
        @NotBlank @Size(max = 1000) String situacao,
        @Schema(description = "Objetivo educacional", minLength = 1, maxLength = 500)
        @NotBlank @Size(max = 500) String objetivoEducacional,
        @Schema(description = "URL HTTP ou HTTPS da fonte", example = "https://www.gov.br/amazul", maxLength = 1000)
        @NotBlank @Size(max = 1000) @HttpUrl String fonteUrl) {
    public CenarioRequest {
        titulo = Text.trim(titulo);
        situacao = Text.trim(situacao);
        objetivoEducacional = Text.trim(objetivoEducacional);
        fonteUrl = Text.trim(fonteUrl);
    }
}
