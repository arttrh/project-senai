package br.com.cantina.senai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Dados do administrador inicial, lidos do ambiente.
 * Ver app.admin-inicial.* em application.properties e o .env.example.
 */
@ConfigurationProperties(prefix = "app.admin-inicial")
public record PropriedadesAdminInicial(
        String nome,
        String cpf,
        String email,
        String senha
) {
    public PropriedadesAdminInicial {
        nome = (nome == null || nome.isBlank()) ? "Administrador" : nome;
        cpf = (cpf == null || cpf.isBlank()) ? "00000000000" : cpf.replaceAll("[^0-9]", "");
    }

    public boolean estaConfigurado() {
        return email != null && !email.isBlank() && senha != null && !senha.isBlank();
    }

    public String emailNormalizado() {
        return email == null ? null : email.trim().toLowerCase();
    }
}
