package br.com.fiap.nexo.alternativa;

import br.com.fiap.nexo.shared.ApiError;
import br.com.fiap.nexo.shared.ApiPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/alternativas")
@Validated
@Tag(name = "Alternativas", description = "Operações de alternativas")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Entrada inválida", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Registro inexistente", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Conflito com registro existente ou dependente", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Falha inesperada", content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class AlternativaController {
    private final AlternativaService service;

    public AlternativaController(AlternativaService service) { this.service = service; }

    @SecurityRequirement(name = "adminToken")
    @ApiResponse(responseCode = "401", description = "Token administrativo ausente ou inválido", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Permissão administrativa necessária", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PostMapping
    @Operation(summary = "Criar alternativa")
    @ApiResponse(responseCode = "201", description = "Registro criado", content = @Content(schema = @Schema(implementation = AlternativaResponse.class)))
    public ResponseEntity<AlternativaResponse> criar(@Valid @RequestBody AlternativaRequest request) {
        AlternativaResponse response = service.criar(request);
        return ResponseEntity.created(URI.create("/alternativas/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar alternativas", description = "Retorna uma página ordenada por ID crescente.")
    @ApiResponse(responseCode = "200", description = "Página de resultados")
    public ApiPage<AlternativaResponse> listar(
            @Parameter(description = "Página a partir de zero", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página de 1 a 50", example = "10") @RequestParam(defaultValue = "10") int size) {
        return service.listar(page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter alternativa por ID")
    @ApiResponse(responseCode = "200", description = "Registro encontrado", content = @Content(schema = @Schema(implementation = AlternativaResponse.class)))
    public AlternativaResponse obter(@Parameter(description = "ID positivo do registro", required = true) @PathVariable @Positive Long id) {
        return service.obter(id);
    }

    @SecurityRequirement(name = "adminToken")
    @ApiResponse(responseCode = "401", description = "Token administrativo ausente ou inválido", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Permissão administrativa necessária", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PutMapping("/{id}")
    @Operation(summary = "Substituir campos de alternativa")
    @ApiResponse(responseCode = "200", description = "Registro atualizado", content = @Content(schema = @Schema(implementation = AlternativaResponse.class)))
    public AlternativaResponse atualizar(
            @Parameter(description = "ID positivo do registro", required = true) @PathVariable @Positive Long id,
            @Valid @RequestBody AlternativaRequest request) {
        return service.atualizar(id, request);
    }

    @SecurityRequirement(name = "adminToken")
    @ApiResponse(responseCode = "401", description = "Token administrativo ausente ou inválido", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Permissão administrativa necessária", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir alternativa")
    @ApiResponse(responseCode = "204", description = "Registro excluído", content = @Content)
    public ResponseEntity<Void> excluir(@Parameter(description = "ID positivo do registro", required = true) @PathVariable @Positive Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
