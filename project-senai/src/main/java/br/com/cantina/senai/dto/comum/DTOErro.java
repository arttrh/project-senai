package br.com.cantina.senai.dto.comum;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Corpo padrao de erro.
 *
 * O handler antigo respondia sempre com ResponseEntity...build(), ou seja, sem
 * corpo nenhum: o front recebia um status seco e nao tinha como dizer ao
 * usuario o que deu errado.
 */
public record DTOErro(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoInvalido> camposInvalidos
) {
        public record CampoInvalido(String campo, String mensagem) {
        }

        public static DTOErro de(int status, String erro, String mensagem, String caminho) {
                return new DTOErro(LocalDateTime.now(), status, erro, mensagem, caminho, List.of());
        }

        public static DTOErro comCampos(int status, String erro, String mensagem, String caminho,
                                        List<CampoInvalido> campos) {
                return new DTOErro(LocalDateTime.now(), status, erro, mensagem, caminho, campos);
        }
}
