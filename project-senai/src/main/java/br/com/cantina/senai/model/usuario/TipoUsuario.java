package br.com.cantina.senai.model.usuario;

/**
 * Perfis de acesso. USUARIO cobre aluno e professor (quem compra),
 * FUNCIONARIO atende a cantina e ADMIN administra o sistema.
 */
public enum TipoUsuario {
    USUARIO,
    FUNCIONARIO,
    ADMIN;

    /** Nome da role no Spring Security. */
    public String getRole() {
        return "ROLE_" + name();
    }
}
