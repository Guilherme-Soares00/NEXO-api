package br.com.fiap.nexo.cenario;

import br.com.fiap.nexo.shared.ApiError;
import br.com.fiap.nexo.shared.ApiPage;
import br.com.fiap.nexo.alternativa.AlternativaResponse;
import br.com.fiap.nexo.alternativa.AlternativaDoCenarioRequest;
import br.com.fiap.nexo.alternativa.AlternativaService;
import br.com.fiap.nexo.mitofato.MitoFatoService;
import br.com.fiap.nexo.mitofato.MitoFatoResponse;
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
@RequestMapping("/cenarios")
@Validated
@Tag(name = "Cenários", description = "Operações de cenários")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Entrada inválida", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Registro inexistente", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Conflito com registro existente ou dependente", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Falha inesperada", content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class CenarioController {
    private final CenarioService service;
    private final AlternativaService alternativaService;
    private final MitoFatoService mitoFatoService;

    public CenarioController(CenarioService service, AlternativaService alternativaService, MitoFatoService mitoFatoService) {
        this.service = service;
        this.alternativaService = alternativaService;
        this.mitoFatoService = mitoFatoService;
    }

    @SecurityRequirement(name = "adminToken")
    @ApiResponse(responseCode = "401", description = "Token administrativo ausente ou inválido", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Permissão administrativa necessária", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PostMapping
    @Operation(summary = "Criar cenário")
    @ApiResponse(responseCode = "201", description = "Registro criado", content = @Content(schema = @Schema(implementation = CenarioResponse.class)))
    public ResponseEntity<CenarioResponse> criar(@Valid @RequestBody CenarioRequest request) {
        CenarioResponse response = service.criar(request);
        return ResponseEntity.created(URI.create("/cenarios/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar cenários", description = "Retorna uma página ordenada por ID crescente.")
    @ApiResponse(responseCode = "200", description = "Página de resultados")
    public ApiPage<CenarioResponse> listar(
            @Parameter(description = "Página a partir de zero", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página de 1 a 50", example = "10") @RequestParam(defaultValue = "10") int size) {
        return service.listar(page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter cenário por ID")
    @ApiResponse(responseCode = "200", description = "Registro encontrado", content = @Content(schema = @Schema(implementation = CenarioResponse.class)))
    public CenarioResponse obter(@Parameter(description = "ID positivo do registro", required = true) @PathVariable @Positive Long id) {
        return service.obter(id);
    }

    @GetMapping("/{id}/jogo")
    @Operation(summary = "Obter cenário apto para o jogo", description = "Exige alternativas A e B e ao menos um Mito ou Fato. Cenário incompleto retorna 409; o CRUD continua permitindo montagem gradual.")
    @ApiResponse(responseCode = "200", description = "Cenário completo", content = @Content(schema = @Schema(implementation = CenarioJogoResponse.class)))
    public CenarioJogoResponse jogo(@Parameter(description = "ID positivo do cenário", required = true) @PathVariable @Positive Long id) {
        return service.obterParaJogo(id);
    }

    @GetMapping("/{id}/mitos-fatos")
    @Operation(summary = "Listar mitos e fatos do cenário", description = "Conteúdo da descoberta após a decisão; o mesmo cenário fornece os mesmos itens para A e B. Não há controle de etapa por autenticação.")
    @ApiResponse(responseCode = "200", description = "Página de mitos e fatos")
    public ApiPage<MitoFatoResponse> mitosFatos(
            @Parameter(description = "ID positivo do cenário", required = true) @PathVariable @Positive Long id,
            @Parameter(description = "Página a partir de zero", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página de 1 a 50", example = "10") @RequestParam(defaultValue = "10") int size) {
        return mitoFatoService.listarPorCenario(id, page, size);
    }

    @SecurityRequirement(name = "adminToken")
    @ApiResponse(responseCode = "401", description = "Token administrativo ausente ou inválido", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Permissão administrativa necessária", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PutMapping("/{id}")
    @Operation(summary = "Substituir campos de cenário")
    @ApiResponse(responseCode = "200", description = "Registro atualizado", content = @Content(schema = @Schema(implementation = CenarioResponse.class)))
    public CenarioResponse atualizar(
            @Parameter(description = "ID positivo do registro", required = true) @PathVariable @Positive Long id,
            @Valid @RequestBody CenarioRequest request) {
        return service.atualizar(id, request);
    }

    @SecurityRequirement(name = "adminToken")
    @ApiResponse(responseCode = "401", description = "Token administrativo ausente ou inválido", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Permissão administrativa necessária", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir cenário")
    @ApiResponse(responseCode = "204", description = "Registro excluído", content = @Content)
    public ResponseEntity<Void> excluir(@Parameter(description = "ID positivo do registro", required = true) @PathVariable @Positive Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/busca")
    @Operation(summary = "Buscar cenários pelo título", description = "Busca parcial sem distinguir maiúsculas e minúsculas; texto recebe trim.")
    @ApiResponse(responseCode = "200", description = "Página de cenários encontrados")
    public ApiPage<CenarioResponse> buscar(
            @Parameter(description = "Trecho obrigatório do título, de 1 a 150 caracteres", required = true) @RequestParam String texto,
            @Parameter(description = "Página a partir de zero", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página de 1 a 50", example = "10") @RequestParam(defaultValue = "10") int size) {
        return service.buscar(texto, page, size);
    }

    @SecurityRequirement(name = "adminToken")
    @ApiResponse(responseCode = "401", description = "Token administrativo ausente ou inválido", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Permissão administrativa necessária", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PostMapping("/{id}/alternativas")
    @Operation(summary = "Criar alternativa de um cenário",
            description = "Usa o cenário da rota e o mesmo serviço JPA do POST /alternativas.")
    @ApiResponse(responseCode = "201", description = "Alternativa criada",
            content = @Content(schema = @Schema(implementation = AlternativaResponse.class)))
    public ResponseEntity<AlternativaResponse> criarAlternativa(
            @Parameter(description = "ID positivo do cenário", required = true) @PathVariable @Positive Long id,
            @Valid @RequestBody AlternativaDoCenarioRequest request) {
        AlternativaResponse response = alternativaService.criar(request.paraCenario(id));
        return ResponseEntity.created(URI.create("/alternativas/" + response.id())).body(response);
    }

    @GetMapping("/{id}/alternativas")
    @Operation(summary = "Listar alternativas de um cenário", description = "Retorna alternativas por rótulo crescente (A antes de B); cenário inexistente retorna 404.")
    @ApiResponse(responseCode = "200", description = "Página de alternativas")
    public ApiPage<AlternativaResponse> alternativas(
            @Parameter(description = "ID positivo do cenário", required = true) @PathVariable @Positive Long id,
            @Parameter(description = "Página a partir de zero", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página de 1 a 50", example = "10") @RequestParam(defaultValue = "10") int size) {
        return service.listarAlternativas(id, page, size);
    }
}
