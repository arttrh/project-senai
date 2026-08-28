package br.com.cantina.senai.integracao;

import br.com.cantina.senai.model.estoque.Estoque;
import br.com.cantina.senai.model.produto.CategoriaProduto;
import br.com.cantina.senai.model.produto.Produto;
import br.com.cantina.senai.model.usuario.TipoUsuario;
import br.com.cantina.senai.model.usuario.Usuario;
import br.com.cantina.senai.repository.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Base dos testes de integracao: sobe o contexto inteiro contra H2 com as
 * mesmas migrations do Flyway que rodam em producao, entao o que passa aqui
 * passou pelo schema real, pelo Spring Security e pelo tratamento de erro.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class BaseIntegracao {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;

    @Autowired protected UsuarioRepository usuarioRepository;
    @Autowired protected ProdutoRepository produtoRepository;
    @Autowired protected EstoqueRepository estoqueRepository;
    @Autowired protected PedidoRepository pedidoRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    /** CPF e e-mail sao unicos no banco; o contador evita colisao entre testes. */
    private static final AtomicLong SEQUENCIA = new AtomicLong(1);

    protected static final String SENHA_PADRAO = "senhaSegura1";

    protected Usuario criarUsuario(TipoUsuario tipo) {
        long numero = SEQUENCIA.getAndIncrement();

        Usuario usuario = new Usuario();
        usuario.setNome("Pessoa " + numero);
        usuario.setCpf(String.format("%011d", numero));
        usuario.setTelefone("(11) 90000-0000");
        usuario.setEmail("pessoa" + numero + "@senai.br");
        usuario.setSenha(passwordEncoder.encode(SENHA_PADRAO));
        usuario.setTipoUsuario(tipo);
        usuario.setAtivo(true);
        return usuarioRepository.save(usuario);
    }

    protected Produto criarProduto(String nome, String preco, int quantidade) {
        Produto produto = new Produto();
        produto.setNomeProduto(nome + "-" + SEQUENCIA.getAndIncrement());
        produto.setDescricaoProduto("Descricao de " + nome);
        produto.setPreco(new BigDecimal(preco));
        produto.setCategoria(CategoriaProduto.LANCHE);
        produto.setProdutoAtivo(true);
        produtoRepository.save(produto);

        estoqueRepository.save(new Estoque(produto, quantidade));
        return produto;
    }

    protected String json(Object valor) throws Exception {
        return objectMapper.writeValueAsString(valor);
    }
}
