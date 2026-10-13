package br.com.fiap.nexo.shared;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;

public final class OracleErrorTranslator {
    private OracleErrorTranslator() {}

    public static Optional<Failure> translate(Throwable error) {
        ArrayDeque<Throwable> pending = new ArrayDeque<>();
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        pending.add(error);
        boolean databaseError = false;
        while (!pending.isEmpty()) {
            Throwable current = pending.removeFirst();
            if (!visited.add(current)) continue;
            if (current instanceof SQLException sql) {
                databaseError = true;
                Failure failure = switch (sql.getErrorCode()) {
                    case 20001 -> new Failure(HttpStatus.BAD_REQUEST, "Dados inválidos para a operação.");
                    case 20002 -> new Failure(HttpStatus.NOT_FOUND, "Registro não encontrado.");
                    case 20003 -> new Failure(HttpStatus.NOT_FOUND, "Registro relacionado não encontrado.");
                    case 1, 2291, 2292, 20004 -> new Failure(HttpStatus.CONFLICT,
                            "A operação conflita com registros existentes ou dependentes.");
                    default -> null;
                };
                if (failure != null) return Optional.of(failure);
                if (sql.getNextException() != null) pending.addLast(sql.getNextException());
            }
            if (current.getCause() != null) pending.addLast(current.getCause());
        }
        return databaseError
                ? Optional.of(new Failure(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu uma falha inesperada."))
                : Optional.empty();
    }

    public record Failure(HttpStatus status, String message) {}
}
