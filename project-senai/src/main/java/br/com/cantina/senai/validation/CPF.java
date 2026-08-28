package br.com.cantina.senai.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Valida CPF de verdade (digitos verificadores), nao so o formato.
 *
 * O regex antigo (\d{3}\d{3}\d{3}-\d{2}) aceitava "000000000-00" e recusava
 * "000.000.000-00", que era justamente o formato mostrado no placeholder do
 * formulario de cadastro.
 */
@Documented
@Constraint(validatedBy = CPFValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface CPF {

    String message() default "CPF invalido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
