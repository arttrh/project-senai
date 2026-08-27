package br.com.cantina.senai.dto.usuario;

import br.com.cantina.senai.model.usuario.TipoUsuario;
import br.com.cantina.senai.model.usuario.Usuario;

public record DTOListagemUsuario(
        Long idUsuario,
        String nome,
        String telefone,
        String email,
        TipoUsuario tipoUsuario,
        boolean ativo
) {
        public DTOListagemUsuario(Usuario usuario) {
                this(
                        usuario.getIdUsuario(),
                        usuario.getNome(),
                        usuario.getTelefone(),
                        usuario.getEmail(),
                        usuario.getTipoUsuario(),
                        usuario.isAtivo()
                );
        }
}
