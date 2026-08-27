package br.com.cantina.senai.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Aceita CPF com ou sem mascara e confere os dois digitos verificadores.
 * Rejeita as sequencias repetidas (111.111.111-11 etc.), que passam no
 * calculo mas nunca sao emitidas.
 */
public class CPFValidator implements ConstraintValidator<CPF, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        if (valor == null || valor.isBlank()) {
            // Ausencia e responsabilidade do @NotBlank, nao deste validador.
            return true;
        }

        String digitos = valor.replaceAll("[^0-9]", "");
        if (digitos.length() != 11 || digitos.chars().distinct().count() == 1) {
            return false;
        }

        return conferirDigito(digitos, 9, 10) && conferirDigito(digitos, 10, 11);
    }

    private boolean conferirDigito(String digitos, int posicao, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < posicao; i++) {
            soma += Character.getNumericValue(digitos.charAt(i)) * (pesoInicial - i);
        }
        int resto = soma % 11;
        int esperado = (resto < 2) ? 0 : 11 - resto;
        return Character.getNumericValue(digitos.charAt(posicao)) == esperado;
    }
}
