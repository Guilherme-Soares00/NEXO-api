package br.com.fiap.nexo.shared;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = HttpUrlValidator.class)
public @interface HttpUrl {
    String message() default "deve ser uma URL HTTP ou HTTPS válida";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
