/*
 * Painel da cantina.
 *
 * A versao anterior nao funcionava por quatro motivos:
 *  - lia p.id, p.status e p.valorTotal, campos que a entidade Pedido nunca
 *    teve (os nomes reais eram idPedido e statusPedido, e valor nao existia);
 *  - chamava PATCH /api/funcionario/pedidos/{id}/status, endpoint inexistente;
 *  - o HTML chamava abrirModalEstoque, salvarEstoque, abrirModalProduto,
 *    salvarProduto e fecharModal, funcoes que nao estavam definidas em lugar
 *    nenhum: todo clique nos botoes de estoque virava ReferenceError;
 *  - nao mandava CSRF, entao qualquer escrita seria recusada.
 */

let pedidos = [];
let estoque = [];
let statusFiltro = 'todos';
let estoqueEditando = null;

const ROTULO_STATUS = {
    CRIADO: 'Pendente',
    EM_PREPARACAO: 'Em preparação',
    PRONTO: 'Pronto',
    FINALIZADO: 'Entregue',
    CANCELADO: 'Cancelado'
};

/* ── PEDIDOS ─────────────────────────────────────────────────────────────── */

async function carregarPedidos() {
    try {
        pedidos = await App.requisitar('/api/funcionario/pedidos');
        renderPedidos();
    } catch (erro) {
        console.error('Erro ao buscar pedidos:', erro);
    }
}

function renderPedidos() {
    const lista = document.getElementById('listaPedidos');
    const vazio = document.getElementById('vazioPedidos');
    const busca = (document.getElementById('buscaPedido').value || '').toLowerCase();

    const filtrados = pedidos.filter(pedido => {
        const combinaStatus = statusFiltro === 'todos' || pedido.status === statusFiltro;
        const combinaBusca = String(pedido.idPedido).includes(busca)
            || (pedido.nomeUsuario || '').toLowerCase().includes(busca);
        return combinaStatus && combinaBusca;
    });

    document.getElementById('countPedidos').textContent =
        pedidos.filter(p => p.status === 'CRIADO').length;

    vazio.style.display = filtrados.length === 0 ? 'block' : 'none';
    lista.innerHTML = filtrados.map(cartaoDoPedido).join('');
}

function cartaoDoPedido(pedido) {
    const itens = (pedido.itens || [])
        .map(i => `${App.escapar(i.nomeProduto)} x${i.quantidade}`)
        .join('<br>') || 'Pedido sem itens';

    return `
    <div class="pedido-card ${pedido.status.toLowerCase()}">
        <div class="pedido-head">
            <span class="pedido-num">#${String(pedido.idPedido).padStart(4, '0')}</span>
            <span class="pedido-status status-${pedido.status.toLowerCase()}">
                ${ROTULO_STATUS[pedido.status] || pedido.status}
            </span>
        </div>
        <div class="pedido-body">
            <div class="pedido-item"><strong>${App.escapar(pedido.nomeUsuario)}</strong></div>
            <div class="pedido-item">${itens}</div>
            <div class="pedido-meta">
                ${App.dataHora(pedido.dataPedido)} · ${App.escapar(pedido.formaPagamento)}
                ${pedido.observacao ? '<br>Obs: ' + App.escapar(pedido.observacao) : ''}
            </div>
        </div>
        <div class="pedido-footer">
            <span class="pedido-valor">${App.moeda(pedido.valorTotal)}</span>
        </div>
        <div class="pedido-acoes">${botoesDoPedido(pedido)}</div>
    </div>`;
}

/**
 * Os botoes seguem as transicoes que o back aceita (ver StatusPedido no Java),
 * para a tela nunca oferecer uma acao que resultaria em 409.
 */
function botoesDoPedido(pedido) {
    const cancelar = `<button class="btn-acao cancelar"
            onclick="alterarStatus(${pedido.idPedido}, 'CANCELADO')">Cancelar</button>`;

    switch (pedido.status) {
        case 'CRIADO':
            return `<button class="btn-acao pronto"
                        onclick="alterarStatus(${pedido.idPedido}, 'EM_PREPARACAO')">
                        Iniciar preparo</button>${cancelar}`;
        case 'EM_PREPARACAO':
            return `<button class="btn-acao pronto"
                        onclick="alterarStatus(${pedido.idPedido}, 'PRONTO')">
                        Marcar como pronto</button>${cancelar}`;
        case 'PRONTO':
            return `<button class="btn-acao entregar"
                        onclick="alterarStatus(${pedido.idPedido}, 'FINALIZADO')">
                        Confirmar entrega</button>${cancelar}`;
        default:
            return '<button class="btn-acao" disabled style="opacity:0.4">Encerrado</button>';
    }
}

async function alterarStatus(idPedido, novoStatus) {
    try {
        await App.requisitar(`/api/funcionario/pedidos/${idPedido}/status`, {
            method: 'PATCH',
            body: { status: novoStatus }
        });
        App.toast('Pedido atualizado.');
        await carregarPedidos();
        // Cancelamento devolve itens ao estoque: a aba precisa refletir isso.
        if (novoStatus === 'CANCELADO') {
            await carregarEstoque();
        }
    } catch (erro) {
        App.toast(erro.message, 'erro');
    }
}

/* ── ESTOQUE ─────────────────────────────────────────────────────────────── */

async function carregarEstoque() {
    try {
        estoque = await App.requisitar('/api/funcionario/estoque');
        renderEstoque();
    } catch (erro) {
        console.error('Erro ao buscar estoque:', erro);
    }
}

function renderEstoque() {
    const corpo = document.getElementById('tabelaEstoque');
    const vazio = document.getElementById('vazioEstoque');
    const busca = (document.getElementById('buscaEstoque').value || '').toLowerCase();

    const filtrados = estoque.filter(linha =>
        (linha.nomeProduto || '').toLowerCase().includes(busca));

    vazio.style.display = filtrados.length === 0 ? 'block' : 'none';
    corpo.innerHTML = filtrados.map(linha => `
        <tr>
            <td><p class="estoque-nome">${App.escapar(linha.nomeProduto)}</p></td>
            <td><p class="estoque-desc">${App.escapar(linha.descricaoProduto || '-')}</p></td>
            <td>${App.moeda(linha.preco)}</td>
            <td><span class="qtd-badge ${classeDaQuantidade(linha.quantidade)}">
                ${linha.quantidade} un.</span></td>
            <td><span class="status-badge ${linha.produtoAtivo ? 'badge-ok' : 'badge-zero'}">
                ${linha.produtoAtivo ? 'Ativo' : 'Inativo'}</span></td>
            <td><button class="btn-editar" onclick="abrirModalEstoque(${linha.idEstoque})">
                Editar</button></td>
        </tr>`).join('');
}

function classeDaQuantidade(quantidade) {
    if (quantidade <= 0) {
        return 'qtd-zero';
    }
    return quantidade <= 5 ? 'qtd-low' : 'qtd-ok';
}

/* ── MODAIS (as funcoes que o HTML chamava e nao existiam) ────────────────── */

function abrirModalEstoque(idEstoque) {
    const linha = estoque.find(e => e.idEstoque === idEstoque);
    if (!linha) {
        return;
    }
    estoqueEditando = idEstoque;
    document.getElementById('modalProdutoNome').textContent = linha.nomeProduto;
    document.getElementById('inputQtd').value = linha.quantidade;
    document.getElementById('modalEstoque').classList.add('show');
}

async function salvarEstoque() {
    const quantidade = Number(document.getElementById('inputQtd').value);
    if (!Number.isInteger(quantidade) || quantidade < 0) {
        App.toast('Informe uma quantidade válida.', 'erro');
        return;
    }
    try {
        await App.requisitar(`/api/funcionario/estoque/${estoqueEditando}`, {
            method: 'PUT',
            body: { quantidade: quantidade }
        });
        fecharModal('modalEstoque');
        App.toast('Estoque atualizado.');
        await carregarEstoque();
    } catch (erro) {
        App.toast(erro.message, 'erro');
    }
}

function abrirModalProduto() {
    ['inputNomeProduto', 'inputDescProduto', 'inputQtdProduto', 'inputPrecoProduto']
        .forEach(id => {
            const campo = document.getElementById(id);
            if (campo) {
                campo.value = '';
            }
        });
    document.getElementById('inputCategoriaProduto').value = 'LANCHE';
    document.getElementById('modalProduto').classList.add('show');
}

async function salvarProduto() {
    const corpo = {
        nomeProduto: document.getElementById('inputNomeProduto').value.trim(),
        descricaoProduto: document.getElementById('inputDescProduto').value.trim(),
        preco: Number(document.getElementById('inputPrecoProduto').value),
        categoria: document.getElementById('inputCategoriaProduto').value,
        quantidadeInicial: Number(document.getElementById('inputQtdProduto').value)
    };

    if (!corpo.nomeProduto) {
        App.toast('Informe o nome do produto.', 'erro');
        return;
    }
    if (!(corpo.preco > 0)) {
        App.toast('Informe um preço maior que zero.', 'erro');
        return;
    }

    try {
        await App.requisitar('/api/funcionario/produtos', { method: 'POST', body: corpo });
        fecharModal('modalProduto');
        App.toast('Produto cadastrado.');
        await carregarEstoque();
    } catch (erro) {
        App.toast(erro.message, 'erro');
    }
}

function fecharModal(id) {
    document.getElementById(id).classList.remove('show');
}

/* ── NAVEGACAO ───────────────────────────────────────────────────────────── */

function filtrarStatus(status, botao) {
    statusFiltro = status;
    document.querySelectorAll('.filtro-status').forEach(b => b.classList.remove('active'));
    botao.classList.add('active');
    renderPedidos();
}

function mostrarTab(id, botao) {
    document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.getElementById('tab-' + id).classList.add('active');
    botao.classList.add('active');
}

async function iniciar() {
    await carregarPedidos();
    await carregarEstoque();
    setInterval(carregarPedidos, 15000);
}

iniciar();
