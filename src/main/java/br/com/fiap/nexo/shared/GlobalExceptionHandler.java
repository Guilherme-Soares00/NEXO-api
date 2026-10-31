package br.com.fiap.nexo.shared;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldError(error.getField(), error.getDefaultMessage())).distinct().toList();
        return response(HttpStatus.BAD_REQUEST, "Dados inválidos.", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> invalidConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        List<ApiError.FieldError> errors = ex.getConstraintViolations().stream().map(violation -> {
            String path = violation.getPropertyPath().toString();
            return new ApiError.FieldError(path.substring(path.lastIndexOf('.') + 1), violation.getMessage());
        }).toList();
        return response(HttpStatus.BAD_REQUEST, "Dados inválidos.", request, errors);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class, HandlerMethodValidationException.class})
    public ResponseEntity<ApiError> invalidInput(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Entrada inválida. Verifique campos, tipos e parâmetros obrigatórios.", request, List.of());
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ApiError> invalidValue(InvalidInputException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiError> unknownRoute(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "Recurso não encontrado.", request, List.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> conflict(ConflictException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrityConflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        return databaseResponse(ex, request, HttpStatus.CONFLICT,
                "A operação conflita com registros existentes ou dependentes.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> methodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        ResponseEntity<ApiError> response = response(HttpStatus.METHOD_NOT_ALLOWED,
                "Método HTTP não permitido.", request, List.of());
        return ResponseEntity.status(response.getStatusCode()).headers(ex.getHeaders()).body(response.getBody());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> unsupportedMedia(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de conteúdo não suportado.", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest request) {
        return databaseResponse(ex, request, HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu uma falha inesperada.");
    }

    private ResponseEntity<ApiError> databaseResponse(Exception ex, HttpServletRequest request,
                                                     HttpStatus fallbackStatus, String fallbackMessage) {
        OracleErrorTranslator.Failure failure = OracleErrorTranslator.translate(ex)
                .orElse(new OracleErrorTranslator.Failure(fallbackStatus, fallbackMessage));
        return response(failure.status(), failure.message(), request, List.of());
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String message, HttpServletRequest request, List<ApiError.FieldError> errors) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI(), errors));
    }
}
