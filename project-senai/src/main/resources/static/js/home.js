/*
 * Cardapio.
 *
 * Mudancas em relacao a versao anterior:
 *  - preco de verdade, vindo de /api/produtos (o DTO nao tinha preco, entao a
 *    tela exibia "R$ --" fixo e o total do carrinho era sempre zero);
 *  - a categoria vem do produto, entao os filtros Lanche/Bebida/Sobremesa
 *    finalmente separam alguma coisa (antes todo item nascia com cat 'todos');
 *  - o botao de adicionar respeita o saldo em estoque;
 *  - o polling de 10s nao apaga mais o que o usuario esta fazendo.
 */

let produtos = [];
let carrinho = App.lerCarrinho();
let categoriaAtual = 'todos';

const EMOJI_POR_CATEGORIA = { LANCHE: '🍔', BEBIDA: '🥤', SOBREMESA: '🍰' };
const FUNDO_POR_CATEGORIA = { LANCHE: 'bg-rosa', BEBIDA: 'bg-azul', SOBREMESA: 'bg-amarelo' };

async function carregarProdutos() {
    try {
        produtos = await App.requisitar('/api/produtos');
        removerItensIndisponiveis();
        renderGrid();
        atualizarCartBar();
    } catch (erro) {
        console.error('Falha ao carregar o cardapio:', erro);
        App.toast(erro.message, 'erro');
    }
}

/** Produto que saiu do cardapio ou zerou o estoque nao pode ficar no carrinho. */
function removerItensIndisponiveis() {
    const disponiveis = new Map(produtos.map(p => [String(p.idProduto), p]));
    let mudou = false;

    Object.keys(carrinho).forEach(id => {
        const produto = disponiveis.get(id);
        if (!produto || !produto.disponivel) {
            delete carrinho[id];
            mudou = true;
        } else if (carrinho[id] > produto.quantidadeEstoque) {
            carrinho[id] = produto.quantidadeEstoque;
            mudou = true;
        }
    });

    if (mudou) {
        App.gravarCarrinho(carrinho);
    }
}

function renderGrid() {
    const grid = document.getElementById('grid');
    const vazio = document.getElementById('vazio');
    const busca = (document.getElementById('busca').value || '').toLowerCase();
    if (!grid) {
        return;
    }

    const filtrados = produtos.filter(p =>
        (categoriaAtual === 'todos' || p.categoria === categoriaAtual) &&
        p.nomeProduto.toLowerCase().includes(busca)
    );

    vazio.style.display = filtrados.length === 0 ? 'block' : 'none';
    grid.innerHTML = filtrados.map(cartaoDoProduto).join('');
}

function cartaoDoProduto(produto) {
    const quantidade = carrinho[produto.idProduto] || 0;
    const emoji = EMOJI_POR_CATEGORIA[produto.categoria] || '🍽️';
    const fundo = FUNDO_POR_CATEGORIA[produto.categoria] || 'bg-rosa';
    const noLimite = quantidade >= produto.quantidadeEstoque;

    const controles = quantidade > 0
        ? `<div class="card-controles">
               <button class="btn-ctrl btn-menos" onclick="removerItem(${produto.idProduto})"
                       aria-label="Remover um">−</button>
               <span class="card-qtd">${quantidade}</span>
               <button class="btn-ctrl btn-mais" onclick="adicionarItem(${produto.idProduto})"
                       ${noLimite ? 'disabled' : ''} aria-label="Adicionar um">+</button>
           </div>`
        : `<button class="btn-add" onclick="adicionarItem(${produto.idProduto})"
                   ${produto.disponivel ? '' : 'disabled'} aria-label="Adicionar ao pedido">+</button>`;

    return `
    <div class="food-card${produto.disponivel ? '' : ' esgotado'}">
        <div class="card-img ${fundo}">
            <span style="font-size:38px">${emoji}</span>
            <span class="badge ${produto.disponivel ? 'badge-ok' : 'badge-nao'}">
                ${produto.disponivel ? 'Disponível' : 'Esgotado'}
            </span>
        </div>
        <div class="card-body">
            <p class="card-nome">${App.escapar(produto.nomeProduto)}</p>
            <p class="card-desc">${App.escapar(produto.descricaoProduto || '')}</p>
            <div class="card-footer">
                <span class="card-preco">${App.moeda(produto.preco)}</span>
                ${controles}
            </div>
        </div>
    </div>`;
}

function produtoPorId(id) {
    return produtos.find(p => p.idProduto === Number(id));
}

function adicionarItem(id) {
    const produto = produtoPorId(id);
    if (!produto || !produto.disponivel) {
        return;
    }
    const atual = carrinho[id] || 0;
    if (atual >= produto.quantidadeEstoque) {
        App.toast(`Só temos ${produto.quantidadeEstoque} de ${produto.nomeProduto}.`, 'erro');
        return;
    }
    carrinho[id] = atual + 1;
    persistirEAtualizar();
}

function removerItem(id) {
    if (!carrinho[id]) {
        return;
    }
    carrinho[id] -= 1;
    if (carrinho[id] <= 0) {
        delete carrinho[id];
    }
    persistirEAtualizar();
}

function persistirEAtualizar() {
    App.gravarCarrinho(carrinho);
    atualizarCartBar();
    renderGrid();
}

/** Agora soma dinheiro de verdade, porque o preco existe no DTO. */
function atualizarCartBar() {
    const barra = document.getElementById('cartBar');
    if (!barra) {
        return;
    }

    let quantidadeTotal = 0;
    let valorTotal = 0;

    Object.entries(carrinho).forEach(([id, quantidade]) => {
        const produto = produtoPorId(id);
        quantidadeTotal += quantidade;
        if (produto) {
            valorTotal += Number(produto.preco) * quantidade;
        }
    });

    document.getElementById('cartQtd').textContent = quantidadeTotal;
    document.getElementById('cartTotal').textContent = App.moeda(valorTotal);
    document.getElementById('cartLabel').textContent = quantidadeTotal === 1 ? 'item' : 'itens';
    barra.classList.toggle('hidden', quantidadeTotal === 0);
}

function setCategoria(categoria, botao) {
    categoriaAtual = categoria;
    document.querySelectorAll('.filtro-btn').forEach(b => b.classList.remove('active'));
    botao.classList.add('active');
    renderGrid();
}

function filtrar() {
    renderGrid();
}

function mostrarDataDeHoje() {
    const elemento = document.getElementById('dataHoje');
    if (elemento) {
        elemento.textContent = new Date().toLocaleDateString('pt-BR', {
            weekday: 'short', day: 'numeric', month: 'long'
        });
    }
}

mostrarDataDeHoje();
carregarProdutos();

// Mantem o saldo em dia sem atrapalhar quem esta montando o pedido.
setInterval(carregarProdutos, 30000);
