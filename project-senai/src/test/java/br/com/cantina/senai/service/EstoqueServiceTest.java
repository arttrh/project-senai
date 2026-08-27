package br.com.cantina.senai.service;

import br.com.cantina.senai.dto.estoque.DTOAtualizarEstoque;
import br.com.cantina.senai.dto.estoque.DTOCadastroEstoque;
import br.com.cantina.senai.exception.EstoqueNaoEncontradoException;
import br.com.cantina.senai.exception.RecursoDuplicadoException;
import br.com.cantina.senai.model.estoque.Estoque;
import br.com.cantina.senai.model.produto.Produto;
import br.com.cantina.senai.repository.EstoqueRepository;
import br.com.cantina.senai.util.Fabrica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EstoqueServiceTest {

    @Mock private EstoqueRepository estoqueRepository;
    @Mock private ProdutoService produtoService;

    private EstoqueService estoqueService;
    private Produto coxinha;

    @BeforeEach
    void preparar() {
        estoqueService = new EstoqueService(estoqueRepository, produtoService);
        coxinha = Fabrica.produto(10L, "Coxinha", "7.50");

        when(produtoService.buscarEntidade(10L)).thenReturn(coxinha);
        when(estoqueRepository.save(any(Estoque.class))).thenAnswer(c -> c.getArgument(0));
    }

    @Test
    @DisplayName("cria a linha de estoque de um produto que ainda nao tem")
    void criaEstoque() {
        estoqueService.cadastrar(new DTOCadastroEstoque(10L, 25));

        verify(estoqueRepository).save(any(Estoque.class));
    }

    @Test
    @DisplayName("recusa uma segunda linha de estoque para o mesmo produto")
    void recusaEstoqueDuplicado() {
        // Duas linhas para o mesmo produto faziam findByProduto_IdProduto
        // estourar NonUniqueResultException no meio de uma venda.
        when(estoqueRepository.existsByProduto_IdProduto(10L)).thenReturn(true);

        assertThatThrownBy(() -> estoqueService.cadastrar(new DTOCadastroEstoque(10L, 25)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("ja possui estoque");
    }

    @Test
    @DisplayName("ajuste manual grava a nova quantidade")
    void ajusteManual() {
        Estoque estoque = Fabrica.estoque(100L, coxinha, 5);
        when(estoqueRepository.buscarPorIdComProduto(100L)).thenReturn(Optional.of(estoque));

        assertThat(estoqueService.atualizar(100L, new DTOAtualizarEstoque(40)).quantidade())
                .isEqualTo(40);
        assertThat(estoque.getQuantidade()).isEqualTo(40);
    }

    @Test
    @DisplayName("estoque inexistente vira 404")
    void estoqueInexistente() {
        when(estoqueRepository.buscarPorIdComProduto(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estoqueService.buscarPorId(99L))
                .isInstanceOf(EstoqueNaoEncontradoException.class);
    }

    @Test
    @DisplayName("baixar e repor mexem no saldo de forma simetrica")
    void baixaEReposicao() {
        Estoque estoque = Fabrica.estoque(100L, coxinha, 10);

        assertThat(estoque.temSaldoPara(10)).isTrue();
        assertThat(estoque.temSaldoPara(11)).isFalse();

        estoque.baixar(4);
        assertThat(estoque.getQuantidade()).isEqualTo(6);

        estoque.repor(4);
        assertThat(estoque.getQuantidade()).isEqualTo(10);
    }
}
