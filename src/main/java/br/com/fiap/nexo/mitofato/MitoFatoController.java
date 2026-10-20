package br.com.fiap.nexo.mitofato;

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
@RequestMapping("/mitos-fatos")
@Validated
@Tag(name = "Mitos e fatos", description = "Conteúdo educativo vinculado ao cenário")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Entrada inválida", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Registro inexistente", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Conflito de integridade", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Falha inesperada", content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class MitoFatoController {
    private final MitoFatoService service;

    public MitoFatoController(MitoFatoService service) { this.service = service; }

    @SecurityRequirement(name = "adminToken")
    @ApiResponse(responseCode = "401", description = "Token administrativo ausente ou inválido", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Permissão administrativa necessária", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PostMapping
    @Operation(summary = "Criar Mito ou Fato", description = "O item pertence ao cenário e é compartilhado pelas alternativas A e B.")
    @ApiResponse(responseCode = "201", description = "Registro criado", content = @Content(schema = @Schema(implementation = MitoFatoResponse.class)))
    public ResponseEntity<MitoFatoResponse> criar(@Valid @RequestBody MitoFatoRequest request) {
        MitoFatoResponse response = service.criar(request);
        return ResponseEntity.created(URI.create("/mitos-fatos/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar mitos e fatos", description = "Retorna uma página ordenada por ID crescente.")
    @ApiResponse(responseCode = "200", description = "Página de resultados")
    public ApiPage<MitoFatoResponse> listar(
            @Parameter(description = "Página a partir de zero", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página de 1 a 50", example = "10") @RequestParam(defaultValue = "10") int size) {
        return service.listar(page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter Mito ou Fato por ID")
    @ApiResponse(responseCode = "200", description = "Registro encontrado", content = @Content(schema = @Schema(implementation = MitoFatoResponse.class)))
    public MitoFatoResponse obter(@Parameter(description = "ID positivo do registro", required = true) @PathVariable @Positive Long id) {
        return service.obter(id);
    }

    @SecurityRequirement(name = "adminToken")
    @ApiResponse(responseCode = "401", description = "Token administrativo ausente ou inválido", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Permissão administrativa necessária", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PutMapping("/{id}")
    @Operation(summary = "Substituir campos de Mito ou Fato")
    @ApiResponse(responseCode = "200", description = "Registro atualizado", content = @Content(schema = @Schema(implementation = MitoFatoResponse.class)))
    public MitoFatoResponse atualizar(
            @Parameter(description = "ID positivo do registro", required = true) @PathVariable @Positive Long id,
            @Valid @RequestBody MitoFatoRequest request) {
        return service.atualizar(id, request);
    }

    @SecurityRequirement(name = "adminToken")
    @ApiResponse(responseCode = "401", description = "Token administrativo ausente ou inválido", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Permissão administrativa necessária", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir Mito ou Fato")
    @ApiResponse(responseCode = "204", description = "Registro excluído", content = @Content)
    public ResponseEntity<Void> excluir(@Parameter(description = "ID positivo do registro", required = true) @PathVariable @Positive Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
