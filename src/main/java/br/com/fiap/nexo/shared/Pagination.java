package br.com.fiap.nexo.shared;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Pagination {
    private Pagination() {}
    public static Pageable of(int page, int size) { return of(page, size, "id"); }
    public static Pageable of(int page, int size, String field) {
        if (page < 0 || size < 1 || size > 50) throw new InvalidInputException("Paginação inválida: page deve ser >= 0 e size entre 1 e 50.");
        if ((long) page * size > Integer.MAX_VALUE) throw new InvalidInputException("Paginação inválida: page multiplicado por size não pode exceder 2147483647.");
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, field));
    }
}
