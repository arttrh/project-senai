package br.com.cantina.senai.integracao;

import br.com.cantina.senai.model.pedido.StatusPedido;
import br.com.cantina.senai.model.produto.Produto;
import br.com.cantina.senai.model.usuario.TipoUsuario;
import br.com.cantina.senai.model.usuario.Usuario;
import br.com.cantina.senai.security.UsuarioAutenticado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Fluxo de compra de ponta a ponta, passando por HTTP, seguranca, service,
 * JPA e o schema real criado pelo Flyway.
 */
@Transactional
class PedidoFluxoIntegracaoTest extends BaseIntegracao {

    @Test
    @DisplayName("o cardapio publica preco, categoria e saldo de cada item")
    void cardapioTrazPreco() throws Exception {
        criarProduto("Coxinha", "7.50", 10);

        mockMvc.perform(get("/api/produtos").with(user(comoCliente())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].preco").exists())
                .andExpect(jsonPath("$[0].categoria").exists())
                .andExpect(jsonPath("$[0].quantidadeEstoque").exists())
                .andExpect(jsonPath("$[0].disponivel").exists());
    }

    @Test
    @DisplayName("pedido de varios itens grava total certo e baixa todo o estoque")
    void pedidoCompleto() throws Exception {
        Produto coxinha = criarProduto("Coxinha", "7.50", 10);
        Produto suco = criarProduto("Suco", "7.00", 10);
        Usuario cliente = criarUsuario(TipoUsuario.USUARIO);

        mockMvc.perform(post("/api/pedidos")
                        .with(user(new UsuarioAutenticado(cliente))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"idProduto":%d,"quantidade":2},
                                          {"idProduto":%d,"quantidade":1}],
                                 "formaPagamento":"PIX","observacao":"sem cebola"}"""
                                .formatted(coxinha.getIdProduto(), suco.getIdProduto())))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.valorTotal").value(22.00))
                .andExpect(jsonPath("$.totalItens").value(3))
                .andExpect(jsonPath("$.status").value("CRIADO"))
                .andExpect(jsonPath("$.itens.length()").value(2));

        assertThat(estoqueRepository.findByProduto_IdProduto(coxinha.getIdProduto())
                .orElseThrow().getQuantidade()).isEqualTo(8);
        assertThat(estoqueRepository.findByProduto_IdProduto(suco.getIdProduto())
                .orElseThrow().getQuantidade()).isEqualTo(9);
    }

    @Test
    @DisplayName("pedido sem estoque e recusado com 409 e nao mexe no saldo")
    void pedidoSemEstoque() throws Exception {
        Produto produto = criarProduto("Pudim", "8.50", 2);
        Usuario cliente = criarUsuario(TipoUsuario.USUARIO);

        mockMvc.perform(post("/api/pedidos")
                        .with(user(new UsuarioAutenticado(cliente))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"idProduto":%d,"quantidade":5}],
                                 "formaPagamento":"CARTAO"}"""
                                .formatted(produto.getIdProduto())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(
                        org.hamcrest.Matchers.containsString("Estoque insuficiente")));

        assertThat(estoqueRepository.findByProduto_IdProduto(produto.getIdProduto())
                .orElseThrow().getQuantidade()).isEqualTo(2);
    }

    @Test
    @DisplayName("carrinho vazio nao vira pedido")
    void carrinhoVazio() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .with(user(comoCliente())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[],"formaPagamento":"PIX"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.camposInvalidos").isNotEmpty());
    }

    @Test
    @DisplayName("quantidade zero ou negativa e recusada na validacao")
    void quantidadeInvalida() throws Exception {
        Produto produto = criarProduto("Brigadeiro", "3.00", 10);

        mockMvc.perform(post("/api/pedidos")
                        .with(user(comoCliente())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"idProduto":%d,"quantidade":0}],
                                 "formaPagamento":"PIX"}""".formatted(produto.getIdProduto())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("o pedido nasce no nome de quem esta logado, nao de um id fixo")
    void pedidoPertenceAoUsuarioLogado() throws Exception {
        // O controller antigo gravava tudo com idUsuario = 1L.
        Produto produto = criarProduto("Misto", "9.00", 10);
        Usuario cliente = criarUsuario(TipoUsuario.USUARIO);

        mockMvc.perform(post("/api/pedidos")
                        .with(user(new UsuarioAutenticado(cliente))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"idProduto":%d,"quantidade":1}],
                                 "formaPagamento":"DINHEIRO"}""".formatted(produto.getIdProduto())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuario").value(cliente.getIdUsuario()))
                .andExpect(jsonPath("$.nomeUsuario").value(cliente.getNome()));
    }

    @Test
    @DisplayName("cliente nao le o pedido de outro cliente")
    void naoLePedidoAlheio() throws Exception {
        Long idPedido = criarPedidoPara(criarUsuario(TipoUsuario.USUARIO));

        mockMvc.perform(get("/api/pedidos/" + idPedido).with(user(comoCliente())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ciclo completo: preparo, pronto e entrega pelo funcionario")
    void cicloDeVidaCompleto() throws Exception {
        Long idPedido = criarPedidoPara(criarUsuario(TipoUsuario.USUARIO));
        var funcionario = new UsuarioAutenticado(criarUsuario(TipoUsuario.FUNCIONARIO));

        avancar(idPedido, funcionario, "EM_PREPARACAO").andExpect(status().isOk());
        avancar(idPedido, funcionario, "PRONTO").andExpect(status().isOk());
        avancar(idPedido, funcionario, "FINALIZADO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADO"));
    }

    @Test
    @DisplayName("pular etapas do status devolve 409")
    void naoPulaEtapas() throws Exception {
        Long idPedido = criarPedidoPara(criarUsuario(TipoUsuario.USUARIO));

        avancar(idPedido, new UsuarioAutenticado(criarUsuario(TipoUsuario.FUNCIONARIO)), "FINALIZADO")
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("cancelar devolve o estoque e mantem o pedido no historico")
    void cancelamentoDevolveEstoque() throws Exception {
        Produto produto = criarProduto("Empada", "8.00", 10);
        Usuario cliente = criarUsuario(TipoUsuario.USUARIO);

        MvcResult resultado = mockMvc.perform(post("/api/pedidos")
                        .with(user(new UsuarioAutenticado(cliente))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"idProduto":%d,"quantidade":3}],
                                 "formaPagamento":"PIX"}""".formatted(produto.getIdProduto())))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(estoqueRepository.findByProduto_IdProduto(produto.getIdProduto())
                .orElseThrow().getQuantidade()).isEqualTo(7);

        long idPedido = idDoPedido(resultado);

        mockMvc.perform(post("/api/pedidos/" + idPedido + "/cancelar")
                        .with(user(new UsuarioAutenticado(cliente))).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        assertThat(estoqueRepository.findByProduto_IdProduto(produto.getIdProduto())
                .orElseThrow().getQuantidade())
                .as("o estoque precisa voltar ao valor original")
                .isEqualTo(10);

        assertThat(pedidoRepository.findById(idPedido))
                .as("cancelar nao pode apagar o pedido")
                .isPresent()
                .get()
                .extracting(p -> p.getStatusPedido())
                .isEqualTo(StatusPedido.CANCELADO);
    }

    @Test
    @DisplayName("o cliente ve os proprios pedidos na tela de acompanhamento")
    void historicoDoCliente() throws Exception {
        Usuario cliente = criarUsuario(TipoUsuario.USUARIO);
        criarPedidoPara(cliente);

        mockMvc.perform(get("/api/pedidos/meus").with(user(new UsuarioAutenticado(cliente))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].valorTotal").exists());
    }

    @Test
    @DisplayName("o painel do funcionario devolve DTO, nao a entidade JPA")
    void painelDevolveDto() throws Exception {
        criarPedidoPara(criarUsuario(TipoUsuario.USUARIO));

        // A listagem antiga serializava List<Pedido> direto e expunha o modelo.
        mockMvc.perform(get("/api/funcionario/pedidos")
                        .with(user(new UsuarioAutenticado(criarUsuario(TipoUsuario.FUNCIONARIO)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idPedido").exists())
                .andExpect(jsonPath("$[0].valorTotal").exists())
                .andExpect(jsonPath("$[0].nomeUsuario").exists())
                .andExpect(jsonPath("$[0].usuario").doesNotExist())
                .andExpect(jsonPath("$[0].statusPedido").doesNotExist());
    }

    /* ── auxiliares ──────────────────────────────────────────────────────── */

    private UsuarioAutenticado comoCliente() {
        return new UsuarioAutenticado(criarUsuario(TipoUsuario.USUARIO));
    }

    private Long criarPedidoPara(Usuario cliente) throws Exception {
        Produto produto = criarProduto("Pao de Queijo", "6.00", 20);

        MvcResult resultado = mockMvc.perform(post("/api/pedidos")
                        .with(user(new UsuarioAutenticado(cliente))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"idProduto":%d,"quantidade":1}],
                                 "formaPagamento":"PIX"}""".formatted(produto.getIdProduto())))
                .andExpect(status().isCreated())
                .andReturn();

        return idDoPedido(resultado);
    }

    private long idDoPedido(MvcResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("idPedido").asLong();
    }

    private org.springframework.test.web.servlet.ResultActions avancar(
            Long idPedido, UsuarioAutenticado funcionario, String status) throws Exception {
        return mockMvc.perform(patch("/api/funcionario/pedidos/" + idPedido + "/status")
                .with(user(funcionario)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + status + "\"}"));
    }
}
