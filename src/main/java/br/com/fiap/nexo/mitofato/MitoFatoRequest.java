package br.com.fiap.nexo.mitofato;

import br.com.fiap.nexo.shared.HttpUrl;
import br.com.fiap.nexo.shared.Text;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Conteúdo de Mito ou Fato vinculado ao cenário, compartilhado por A e B", example = """
        {
          "cenarioId": 1,
          "afirmacao": "Afirmação educativa a ser conferida na fonte institucional.",
          "classificacao": "FATO",
          "explicacao": "Justificativa educativa baseada na fonte informada.",
          "fonteUrl": "https://www.gov.br/amazul"
        }
        """)
public record MitoFatoRequest(
        @Schema(description = "ID do cenário existente", example = "1") @NotNull @Positive Long cenarioId,
        @Schema(maxLength = 500) @NotBlank @Size(max = 500) String afirmacao,
        @Schema(allowableValues = {"MITO", "FATO"}) @NotNull Classificacao classificacao,
        @Schema(maxLength = 1000) @NotBlank @Size(max = 1000) String explicacao,
        @Schema(description = "Fonte HTTP ou HTTPS da afirmação", maxLength = 1000)
        @NotBlank @Size(max = 1000) @HttpUrl String fonteUrl) {
    public MitoFatoRequest {
        afirmacao = Text.trim(afirmacao);
        explicacao = Text.trim(explicacao);
        fonteUrl = Text.trim(fonteUrl);
    }
}
