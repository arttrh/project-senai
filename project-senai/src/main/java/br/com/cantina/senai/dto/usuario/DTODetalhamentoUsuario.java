package br.com.cantina.senai.dto.usuario;

import br.com.cantina.senai.model.usuario.TipoUsuario;
import br.com.cantina.senai.model.usuario.Usuario;

/**
 * Saida com os dados de um usuario. O CPF sai mascarado e a senha nunca sai:
 * nenhuma tela precisa do documento completo.
 */
public record DTODetalhamentoUsuario(
        Long idUsuario,
        String nome,
        String cpf,
        String telefone,
        String email,
        TipoUsuario tipoUsuario,
        boolean ativo
) {
        public DTODetalhamentoUsuario(Usuario usuario) {
                this(
                        usuario.getIdUsuario(),
                        usuario.getNome(),
                        mascararCpf(usuario.getCpf()),
                        usuario.getTelefone(),
                        usuario.getEmail(),
                        usuario.getTipoUsuario(),
                        usuario.isAtivo()
                );
        }

        /** 12345678901 -> ***.456.789-**  */
        static String mascararCpf(String cpf) {
                if (cpf == null) {
                        return null;
                }
                String digitos = cpf.replaceAll("[^0-9]", "");
                if (digitos.length() != 11) {
                        return "***";
                }
                return "***." + digitos.substring(3, 6) + "." + digitos.substring(6, 9) + "-**";
        }
}
