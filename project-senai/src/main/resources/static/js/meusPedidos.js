/*
 * Historico de pedidos do cliente.
 *
 * A tela nao existia: depois de confirmar, o usuario nao tinha como acompanhar
 * o proprio pedido nem cancelar enquanto a cantina ainda nao comecara a
 * preparar.
 */

const ROTULO_STATUS = {
    CRIADO: 'Aguardando a cantina',
    EM_PREPARACAO: 'Em preparação',
    PRONTO: 'Pronto para retirada',
    FINALIZADO: 'Entregue',
    CANCELADO: 'Cancelado'
};

async function carregar() {
    const lista = document.getElementById('listaPedidos');
    const vazio = document.getElementById('vazioPedidos');

    try {
        const pedidos = await App.requisitar('/api/pedidos/meus');
        vazio.style.display = pedidos.length === 0 ? 'block' : 'none';
        lista.innerHTML = pedidos.map(cartao).join('');
    } catch (erro) {
        App.toast(erro.message, 'erro');
    }
}

function cartao(pedido) {
    // O cancelamento pelo cliente so vale antes do preparo comecar.
    const podeCancelar = pedido.status === 'CRIADO';

    return `
    <div class="pedido-card ${pedido.status.toLowerCase()}">
        <div class="pedido-head">
            <span class="pedido-num">#${String(pedido.idPedido).padStart(4, '0')}</span>
            <span class="pedido-status status-${pedido.status.toLowerCase()}">
                ${ROTULO_STATUS[pedido.status] || pedido.status}
            </span>
        </div>
        <div class="pedido-body">
            <div class="pedido-meta">${App.dataHora(pedido.dataPedido)}</div>
            <div class="pedido-item">${pedido.totalItens} ${pedido.totalItens === 1 ? 'item' : 'itens'}</div>
        </div>
        <div class="pedido-footer">
            <span class="pedido-valor">${App.moeda(pedido.valorTotal)}</span>
        </div>
        <div class="pedido-acoes">
            ${podeCancelar
                ? `<button class="btn-acao cancelar" onclick="cancelar(${pedido.idPedido})">
                       Cancelar pedido</button>`
                : ''}
        </div>
    </div>`;
}

async function cancelar(idPedido) {
    try {
        await App.requisitar(`/api/pedidos/${idPedido}/cancelar`, { method: 'POST' });
        App.toast('Pedido cancelado.');
        await carregar();
    } catch (erro) {
        App.toast(erro.message, 'erro');
    }
}

carregar();
