package br.com.cantina.senai.util;

import br.com.cantina.senai.model.estoque.Estoque;
import br.com.cantina.senai.model.pedido.FormaPagamento;
import br.com.cantina.senai.model.pedido.Pedido;
import br.com.cantina.senai.model.produto.CategoriaProduto;
import br.com.cantina.senai.model.produto.Produto;
import br.com.cantina.senai.model.usuario.TipoUsuario;
import br.com.cantina.senai.model.usuario.Usuario;

import java.math.BigDecimal;

/** Objetos de dominio prontos, para os testes falarem so do que estao testando. */
public final class Fabrica {

    private Fabrica() {
    }

    public static Usuario usuario(Long id, TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNome("Fulano de Tal");
        usuario.setCpf("52998224725");
        usuario.setTelefone("(11) 99999-9999");
        usuario.setEmail("fulano" + id + "@senai.br");
        usuario.setSenha("$2a$10$hashfake");
        usuario.setTipoUsuario(tipo);
        usuario.setAtivo(true);
        return usuario;
    }

    public static Usuario cliente(Long id) {
        return usuario(id, TipoUsuario.USUARIO);
    }

    public static Produto produto(Long id, String nome, String preco) {
        Produto produto = new Produto();
        produto.setIdProduto(id);
        produto.setNomeProduto(nome);
        produto.setDescricaoProduto("Descricao de " + nome);
        produto.setPreco(new BigDecimal(preco));
        produto.setCategoria(CategoriaProduto.LANCHE);
        produto.setProdutoAtivo(true);
        return produto;
    }

    public static Estoque estoque(Long id, Produto produto, int quantidade) {
        Estoque estoque = new Estoque(produto, quantidade);
        estoque.setIdEstoque(id);
        return estoque;
    }

    public static Pedido pedido(Long id, Usuario usuario) {
        Pedido pedido = new Pedido(usuario, FormaPagamento.PIX, null);
        pedido.setIdPedido(id);
        return pedido;
    }
}
