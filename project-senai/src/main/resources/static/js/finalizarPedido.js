/*
 * Revisao e envio do pedido.
 *
 * O confirmar() antigo montava a lista de itens e mandava so o primeiro
 * (`JSON.stringify(itens[0])`), no formato {produtoId, quantidade, pagamento,
 * observacao}, que nao era o que o back esperava: um carrinho com tres itens
 * virava um pedido de um item so. Agora vai o carrinho inteiro, uma vez, no
 * contrato de DTOCadastroPedido.
 */

let produtos = [];
let carrinho = App.lerCarrinho();
let formaPagamento = 'CARTAO';

async function carregarProdutos() {
    try {
        produtos = await App.requisitar('/api/produtos');
        render();
    } catch (erro) {
        console.error('Falha ao carregar produtos:', erro);
        App.toast(erro.message, 'erro');
    }
}

function produtoPorId(id) {
    return produtos.find(p => p.idProduto === Number(id));
}

function render() {
    const lista = document.getElementById('listaItens');
    const resumo = document.getElementById('resumoLinhas');
    const botao = document.getElementById('btnConfirmar');

    const ids = Object.keys(carrinho).filter(id => carrinho[id] > 0);

    if (ids.length === 0) {
        lista.innerHTML = '<p class="vazio-msg">Nenhum item no carrinho.</p>';
        resumo.innerHTML = '';
        document.getElementById('qtdBadge').textContent = '0 itens';
        document.getElementById('totalValor').textContent = App.moeda(0);
        botao.disabled = true;
        return;
    }

    let total = 0;
    let totalItens = 0;
    const linhasLista = [];
    const linhasResumo = [];

    ids.forEach(id => {
        const produto = produtoPorId(id);
        if (!produto) {
            return;
        }
        const quantidade = carrinho[id];
        const subtotal = Number(produto.preco) * quantidade;
        total += subtotal;
        totalItens += quantidade;

        linhasResumo.push(`
            <div class="resumo-linha">
                <span class="resumo-label">${App.escapar(produto.nomeProduto)} x${quantidade}</span>
                <span class="resumo-valor">${App.moeda(subtotal)}</span>
            </div>`);

        linhasLista.push(`
            <div class="item-row">
                <div class="item-info">
                    <p class="item-nome">${App.escapar(produto.nomeProduto)}</p>
                    <p class="item-sub">${App.moeda(produto.preco)} / unid.</p>
                </div>
                <div class="item-controles">
                    <button class="ctrl-btn" onclick="alterar(${produto.idProduto}, -1)">−</button>
                    <span class="item-qtd">${quantidade}</span>
                    <button class="ctrl-btn" onclick="alterar(${produto.idProduto}, 1)"
                            ${quantidade >= produto.quantidadeEstoque ? 'disabled' : ''}>+</button>
                </div>
                <span class="item-preco">${App.moeda(subtotal)}</span>
            </div>`);
    });

    lista.innerHTML = linhasLista.join('');
    resumo.innerHTML = linhasResumo.join('');
    document.getElementById('qtdBadge').textContent =
        `${totalItens} ${totalItens === 1 ? 'item' : 'itens'}`;
    document.getElementById('totalValor').textContent = App.moeda(total);
    botao.disabled = false;
}

function alterar(id, delta) {
    const produto = produtoPorId(id);
    const nova = (carrinho[id] || 0) + delta;

    if (produto && delta > 0 && nova > produto.quantidadeEstoque) {
        App.toast(`Só temos ${produto.quantidadeEstoque} de ${produto.nomeProduto}.`, 'erro');
        return;
    }
    if (nova <= 0) {
        delete carrinho[id];
    } else {
        carrinho[id] = nova;
    }
    App.gravarCarrinho(carrinho);
    render();
}

function setPag(botao) {
    document.querySelectorAll('.pag-btn').forEach(b => b.classList.remove('selected'));
    botao.classList.add('selected');
    formaPagamento = botao.dataset.forma;
}

async function confirmar() {
    const botao = document.getElementById('btnConfirmar');
    const itens = Object.entries(carrinho)
        .filter(([, quantidade]) => quantidade > 0)
        .map(([idProduto, quantidade]) => ({
            idProduto: Number(idProduto),
            quantidade: quantidade
        }));

    if (itens.length === 0) {
        return;
    }

    // Trava o botao: dois cliques rapidos criavam dois pedidos iguais.
    botao.disabled = true;
    botao.textContent = 'Enviando...';

    try {
        const pedido = await App.requisitar('/api/pedidos', {
            method: 'POST',
            body: {
                itens: itens,
                formaPagamento: formaPagamento,
                observacao: document.querySelector('.obs-input').value || null
            }
        });

        App.limparCarrinho();
        carrinho = {};
        document.getElementById('numPedido').textContent =
            '#' + String(pedido.idPedido).padStart(4, '0');
        document.getElementById('modalSucesso').classList.add('show');
    } catch (erro) {
        App.toast(erro.message, 'erro');
        botao.disabled = false;
        botao.textContent = 'Confirmar pedido';
    }
}

carregarProdutos();
