package br.com.cantina.senai.handler;

import br.com.cantina.senai.dto.comum.DTOErro;
import br.com.cantina.senai.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

/**
 * Tratamento de erro da API.
 *
 * O handler antigo (TradadorGlobalErrors) tinha como primeiro metodo um
 * {@code @ExceptionHandler} sem valor nenhum recebendo Exception: para o Spring
 * isso registra um tratador de TODAS as excecoes, respondendo 404 com corpo
 * vazio. Na pratica erro de validacao, falha de banco e NullPointerException
 * chegavam ao front como "404 Not Found" sem mensagem, e nenhum bug do sistema
 * era visivel.
 *
 * Aqui cada familia de excecao tem o seu status, com corpo explicando o que
 * houve, e so o caso realmente inesperado vira 500 (com o stack trace no log
 * do servidor, nunca na resposta).
 */
@RestControllerAdvice(basePackages = "br.com.cantina.senai.controller.api")
public class TratadorGlobalErrosApi {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalErrosApi.class);

    /** 404: o recurso pedido nao existe. */
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<DTOErro> tratarNaoEncontrado(RecursoNaoEncontradoException e,
                                                       HttpServletRequest requisicao) {
        return montar(HttpStatus.NOT_FOUND, "Recurso nao encontrado", e.getMessage(), requisicao);
    }

    /** 409: a requisicao e valida, mas fere uma regra do dominio. */
    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<DTOErro> tratarRegraDeNegocio(RegraDeNegocioException e,
                                                        HttpServletRequest requisicao) {
        return montar(HttpStatus.CONFLICT, "Regra de negocio", e.getMessage(), requisicao);
    }

    /** 403: existe, mas nao e seu. */
    @ExceptionHandler({AcessoNegadoException.class, AccessDeniedException.class})
    public ResponseEntity<DTOErro> tratarAcessoNegado(Exception e, HttpServletRequest requisicao) {
        return montar(HttpStatus.FORBIDDEN, "Acesso negado",
                "Voce nao tem permissao para esta operacao", requisicao);
    }

    /** 400 com a lista de campos rejeitados, para o front destacar cada um. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DTOErro> tratarValidacao(MethodArgumentNotValidException e,
                                                   HttpServletRequest requisicao) {
        List<DTOErro.CampoInvalido> campos = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> new DTOErro.CampoInvalido(erro.getField(), erro.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest().body(DTOErro.comCampos(
                HttpStatus.BAD_REQUEST.value(),
                "Dados invalidos",
                "Confira os campos informados",
                requisicao.getRequestURI(),
                campos));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<DTOErro> tratarCorpoInvalido(Exception e, HttpServletRequest requisicao) {
        return montar(HttpStatus.BAD_REQUEST, "Requisicao invalida",
                "Nao foi possivel interpretar os dados enviados", requisicao);
    }

    /**
     * 409: duas compras simultaneas do mesmo item. O cliente pode repetir a
     * chamada; nada foi gravado pela metade.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<DTOErro> tratarConcorrencia(OptimisticLockingFailureException e,
                                                      HttpServletRequest requisicao) {
        log.warn("Conflito de concorrencia em {}: {}", requisicao.getRequestURI(), e.getMessage());
        return montar(HttpStatus.CONFLICT, "Conflito de concorrencia",
                "Outro pedido alterou este item agora. Tente novamente.", requisicao);
    }

    /** Rede de seguranca para restricao do banco que o service nao antecipou. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<DTOErro> tratarIntegridade(DataIntegrityViolationException e,
                                                     HttpServletRequest requisicao) {
        log.warn("Violacao de integridade em {}", requisicao.getRequestURI(), e);
        return montar(HttpStatus.CONFLICT, "Conflito de dados",
                "A operacao viola uma restricao do banco de dados", requisicao);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<DTOErro> tratarRotaInexistente(NoHandlerFoundException e,
                                                         HttpServletRequest requisicao) {
        return montar(HttpStatus.NOT_FOUND, "Rota nao encontrada",
                "Nenhum endpoint para " + requisicao.getRequestURI(), requisicao);
    }

    /**
     * Ultimo recurso. A mensagem real fica no log com o correlationId; a
     * resposta e generica para nao vazar detalhe interno ao cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<DTOErro> tratarInesperado(Exception e, HttpServletRequest requisicao) {
        log.error("Erro nao tratado em {} {}", requisicao.getMethod(), requisicao.getRequestURI(), e);
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente em instantes.", requisicao);
    }

    private ResponseEntity<DTOErro> montar(HttpStatus status, String erro, String mensagem,
                                           HttpServletRequest requisicao) {
        return ResponseEntity.status(status)
                .body(DTOErro.de(status.value(), erro, mensagem, requisicao.getRequestURI()));
    }
}
