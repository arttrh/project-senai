/*
 * Utilidades compartilhadas pelas telas.
 *
 * Antes cada arquivo repetia o proprio fetch, tratava erro de um jeito
 * diferente e nenhum enviava o token CSRF, o que passou a ser obrigatorio
 * quando a aplicacao ganhou autenticacao.
 */
const App = (() => {

    /* ── CSRF ──────────────────────────────────────────────────────────────
     * O Spring grava o token no cookie XSRF-TOKEN (legivel pelo JS) e espera
     * ele de volta no header X-XSRF-TOKEN em toda escrita.
     */
    function tokenCsrf() {
        const cookie = document.cookie
            .split(';')
            .map(parte => parte.trim())
            .find(parte => parte.startsWith('XSRF-TOKEN='));
        return cookie ? decodeURIComponent(cookie.substring('XSRF-TOKEN='.length)) : null;
    }

    /**
     * Wrapper de fetch com CSRF, JSON e erro tratado.
     * Lanca Error com a mensagem que o back mandou no corpo padrao de erro,
     * para a tela poder mostrar algo util em vez de "erro".
     */
    async function requisitar(url, opcoes = {}) {
        const cabecalhos = { 'Accept': 'application/json', ...(opcoes.headers || {}) };
        const metodo = (opcoes.method || 'GET').toUpperCase();

        if (opcoes.body !== undefined) {
            cabecalhos['Content-Type'] = 'application/json';
        }
        if (metodo !== 'GET' && metodo !== 'HEAD') {
            const token = tokenCsrf();
            if (token) {
                cabecalhos['X-XSRF-TOKEN'] = token;
            }
        }

        const resposta = await fetch(url, {
            ...opcoes,
            headers: cabecalhos,
            body: opcoes.body !== undefined ? JSON.stringify(opcoes.body) : undefined,
            credentials: 'same-origin'
        });

        if (resposta.status === 401) {
            window.location.href = '/login';
            throw new Error('Sessao expirada');
        }
        if (resposta.status === 204) {
            return null;
        }

        const texto = await resposta.text();
        const corpo = texto ? JSON.parse(texto) : null;

        if (!resposta.ok) {
            throw new Error(mensagemDeErro(corpo, resposta.status));
        }
        return corpo;
    }

    /** Prioriza a lista de campos invalidos, que e a informacao mais util. */
    function mensagemDeErro(corpo, status) {
        if (corpo && Array.isArray(corpo.camposInvalidos) && corpo.camposInvalidos.length > 0) {
            return corpo.camposInvalidos.map(c => `${c.campo}: ${c.mensagem}`).join(' | ');
        }
        if (corpo && corpo.mensagem) {
            return corpo.mensagem;
        }
        return `Falha na requisicao (HTTP ${status})`;
    }

    /* ── Formatacao ─────────────────────────────────────────────────────── */

    function moeda(valor) {
        const numero = Number(valor);
        if (!Number.isFinite(numero)) {
            return 'R$ 0,00';
        }
        return numero.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
    }

    function dataHora(iso) {
        if (!iso) {
            return '';
        }
        return new Date(iso).toLocaleString('pt-BR', {
            day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit'
        });
    }

    /** Escapa antes de jogar em innerHTML: nome de produto vem do banco. */
    function escapar(texto) {
        const div = document.createElement('div');
        div.textContent = texto == null ? '' : String(texto);
        return div.innerHTML;
    }

    function iniciais(nome) {
        if (!nome) {
            return '??';
        }
        return nome.trim().substring(0, 2).toUpperCase();
    }

    /* ── Carrinho (compartilhado entre a home e a tela de fechamento) ────── */

    const CHAVE_CARRINHO = 'carrinho';

    function lerCarrinho() {
        try {
            return JSON.parse(sessionStorage.getItem(CHAVE_CARRINHO) || '{}');
        } catch {
            return {};
        }
    }

    function gravarCarrinho(carrinho) {
        sessionStorage.setItem(CHAVE_CARRINHO, JSON.stringify(carrinho));
    }

    function limparCarrinho() {
        sessionStorage.removeItem(CHAVE_CARRINHO);
    }

    /* ── Toast ──────────────────────────────────────────────────────────── */

    let timerToast = null;

    function toast(mensagem, tipo = 'ok') {
        const elemento = document.getElementById('toast');
        if (!elemento) {
            return;
        }
        elemento.textContent = mensagem;
        elemento.classList.remove('erro');
        if (tipo === 'erro') {
            elemento.classList.add('erro');
        }
        elemento.classList.add('show');

        clearTimeout(timerToast);
        timerToast = setTimeout(() => elemento.classList.remove('show'), 3500);
    }

    return {
        requisitar, moeda, dataHora, escapar, iniciais, toast,
        lerCarrinho, gravarCarrinho, limparCarrinho
    };
})();
