package br.com.fiap.nexo.cenario;

import br.com.fiap.nexo.alternativa.AlternativaResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Cenário apto para exibição no jogo; Mito ou Fato é consultado na etapa de descoberta")
public record CenarioJogoResponse(
        @Schema(description = "Dados do cenário") CenarioResponse cenario,
        @Schema(description = "Alternativas A e B, nessa ordem") List<AlternativaResponse> alternativas) {}
