package br.com.cantina.senai.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DTOAlterarSenha(
        @NotBlank(message = "Senha atual e obrigatoria")
        String senhaAtual,

        @NotBlank(message = "Nova senha e obrigatoria")
        @Size(min = 8, max = 72, message = "Nova senha deve ter no minimo 8 caracteres")
        String novaSenha,

        @NotBlank(message = "Confirmacao e obrigatoria")
        String confirmarSenha
) {
        public boolean confirmacaoConfere() {
                return novaSenha != null && novaSenha.equals(confirmarSenha);
        }
}
