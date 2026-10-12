package br.com.fiap.nexo.shared;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;
import java.util.List;

@Schema(description = "Página de resultados")
public record ApiPage<T>(
        @Schema(description = "Registros da página") List<T> content,
        @Schema(description = "Número da página, começando em zero", example = "0") int page,
        @Schema(description = "Tamanho solicitado", example = "10") int size,
        @Schema(description = "Quantidade total de registros", example = "3") long totalElements,
        @Schema(description = "Quantidade total de páginas", example = "1") int totalPages) {
    public static <T> ApiPage<T> from(Page<T> page) {
        return new ApiPage<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
