package br.com.cantina.senai.service;

import br.com.cantina.senai.dto.produto.*;
import br.com.cantina.senai.exception.ProdutoNaoEncontradoException;
import br.com.cantina.senai.exception.RecursoDuplicadoException;
import br.com.cantina.senai.model.estoque.Estoque;
import br.com.cantina.senai.model.produto.CategoriaProduto;
import br.com.cantina.senai.model.produto.Produto;
import br.com.cantina.senai.repository.EstoqueRepository;
import br.com.cantina.senai.repository.ProdutoRepository;
import br.com.cantina.senai.util.Fabrica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProdutoServiceTest {

    @Mock private ProdutoRepository produtoRepository;
    @Mock private EstoqueRepository estoqueRepository;

    private ProdutoService produtoService;
    private Produto coxinha;

    @BeforeEach
    void preparar() {
        produtoService = new ProdutoService(produtoRepository, estoqueRepository);
        coxinha = Fabrica.produto(10L, "Coxinha", "7.50");

        when(produtoRepository.save(any(Produto.class))).thenAnswer(c -> c.getArgument(0));
        when(estoqueRepository.save(any(Estoque.class))).thenAnswer(c -> c.getArgument(0));
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(coxinha));
    }

    @Test
    @DisplayName("cadastro cria produto e a linha de estoque junto")
    void cadastroCriaEstoque() {
        produtoService.cadastrar(new DTOCadastroProduto("Pastel", "De queijo",
                new BigDecimal("8.00"), CategoriaProduto.LANCHE, 30));

        // Produto sem estoque nunca poderia ser vendido.
        verify(produtoRepository).save(any(Produto.class));
        verify(estoqueRepository).save(any(Estoque.class));
    }

    @Test
    @DisplayName("recusa nome de produto repetido")
    void recusaNomeDuplicado() {
        when(produtoRepository.existsByNomeProduto("Coxinha")).thenReturn(true);

        assertThatThrownBy(() -> produtoService.cadastrar(new DTOCadastroProduto("Coxinha", "x",
                new BigDecimal("7.50"), CategoriaProduto.LANCHE, 10)))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("atualizacao parcial nao apaga o nome quando ele nao e enviado")
    void atualizacaoParcialPreservaNome() {
        // Regressao: o metodo antigo tinha um setNomeProduto solto depois dos
        // ifs, entao um PUT so de preco zerava o nome do produto.
        produtoService.atualizar(10L, new DTOAtualizarProduto(
                null, null, new BigDecimal("9.90"), null, null));

        assertThat(coxinha.getNomeProduto()).isEqualTo("Coxinha");
        assertThat(coxinha.getPreco()).isEqualByComparingTo("9.90");
    }

    @Test
    @DisplayName("atualizacao troca so o que veio preenchido")
    void atualizacaoTrocaCamposEnviados() {
        produtoService.atualizar(10L, new DTOAtualizarProduto(
                "Coxinha Grande", "Nova descricao", null, CategoriaProduto.SOBREMESA, null));

        assertThat(coxinha.getNomeProduto()).isEqualTo("Coxinha Grande");
        assertThat(coxinha.getDescricaoProduto()).isEqualTo("Nova descricao");
        assertThat(coxinha.getCategoria()).isEqualTo(CategoriaProduto.SOBREMESA);
        assertThat(coxinha.getPreco()).isEqualByComparingTo("7.50");
    }

    @Test
    @DisplayName("recusa renomear para o nome de outro produto")
    void recusaRenomearParaNomeExistente() {
        when(produtoRepository.existsByNomeProdutoAndIdProdutoNot("Suco", 10L)).thenReturn(true);

        assertThatThrownBy(() -> produtoService.atualizar(10L,
                new DTOAtualizarProduto("Suco", null, null, null, null)))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("desativa em vez de apagar, para nao quebrar pedidos antigos")
    void desativaSemApagar() {
        produtoService.desativar(10L);

        assertThat(coxinha.isProdutoAtivo()).isFalse();
        verify(produtoRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("a listagem traz preco e marca como indisponivel quem zerou o estoque")
    void listagemTrazPrecoEDisponibilidade() {
        Produto suco = Fabrica.produto(20L, "Suco", "7.00");
        when(produtoRepository.findAllByProdutoAtivoTrueOrderByNomeProdutoAsc())
                .thenReturn(List.of(coxinha, suco));
        when(estoqueRepository.buscarTodosComProduto()).thenReturn(List.of(
                Fabrica.estoque(100L, coxinha, 5),
                Fabrica.estoque(200L, suco, 0)));

        List<DTOListagemProduto> listagem = produtoService.listarDisponiveis();

        assertThat(listagem).hasSize(2);
        assertThat(listagem.getFirst().preco()).isEqualByComparingTo("7.50");
        assertThat(listagem.getFirst().disponivel()).isTrue();
        assertThat(listagem.get(1).disponivel())
                .as("produto com saldo zero nao pode aparecer como disponivel")
                .isFalse();
    }

    @Test
    @DisplayName("produto sem linha de estoque aparece com saldo zero, nao quebra")
    void produtoSemEstoqueNaoQuebraListagem() {
        when(produtoRepository.findAllByProdutoAtivoTrueOrderByNomeProdutoAsc())
                .thenReturn(List.of(coxinha));
        when(estoqueRepository.buscarTodosComProduto()).thenReturn(List.of());

        List<DTOListagemProduto> listagem = produtoService.listarDisponiveis();

        assertThat(listagem.getFirst().quantidadeEstoque()).isZero();
        assertThat(listagem.getFirst().disponivel()).isFalse();
    }

    @Test
    @DisplayName("produto inexistente vira 404")
    void produtoInexistente() {
        when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.buscarPorId(99L))
                .isInstanceOf(ProdutoNaoEncontradoException.class);
    }
}
