/*
 * Configuracoes da conta.
 *
 * Este arquivo nao existia: o configuracaoUsuario.html referenciava
 * /js/configuracaoUsuario.js, o servidor respondia 404 e nenhum botao da tela
 * (trocar de secao, abrir o modal de exclusao, validar a senha) funcionava.
 */

function mostrarSecao(nome, botao) {
    document.querySelectorAll('.secao').forEach(s => s.classList.remove('active'));
    document.querySelectorAll('.side-btn').forEach(b => b.classList.remove('active'));
    document.getElementById('secao-' + nome).classList.add('active');
    botao.classList.add('active');
}

/* ── Senha ───────────────────────────────────────────────────────────────── */

/**
 * Confere no cliente o que o back tambem confere: as duas checagens existem de
 * proposito, a do servidor e a que vale.
 */
function validarSenha(evento) {
    const nova = document.getElementById('novaSenha').value;
    const confirmar = document.getElementById('confirmarSenha').value;

    if (nova.length < 8) {
        evento.preventDefault();
        App.toast('A nova senha precisa de no mínimo 8 caracteres.', 'erro');
        return false;
    }
    if (nova !== confirmar) {
        evento.preventDefault();
        App.toast('A confirmação não confere com a nova senha.', 'erro');
        return false;
    }
    return true;
}

/* ── Exclusao de conta ───────────────────────────────────────────────────── */

function confirmarExclusao() {
    document.getElementById('modalExclusao').classList.add('show');
}

function fecharModal() {
    document.getElementById('modalExclusao').classList.remove('show');
}

async function excluirConta() {
    const id = document.body.dataset.usuarioId;
    try {
        await App.requisitar(`/api/usuarios/${id}`, { method: 'DELETE' });
        window.location.href = '/logout';
    } catch (erro) {
        fecharModal();
        App.toast(erro.message, 'erro');
    }
}

/* ── Avatar ──────────────────────────────────────────────────────────────── */

function previewFoto(evento) {
    const arquivo = evento.target.files[0];
    if (!arquivo) {
        return;
    }
    // Ainda nao ha upload no back: o preview e so visual, e some ao recarregar.
    const url = URL.createObjectURL(arquivo);
    const avatar = document.getElementById('avatarGrande');
    avatar.style.backgroundImage = `url(${url})`;
    avatar.style.backgroundSize = 'cover';
    avatar.textContent = '';
    App.toast('Pré-visualização aplicada (o envio da foto ainda não está disponível).');
}

/* ── Mensagens vindas do redirect do servidor ────────────────────────────── */

document.addEventListener('DOMContentLoaded', () => {
    const mensagem = document.body.dataset.mensagem;
    const tipo = document.body.dataset.mensagemTipo;
    if (mensagem) {
        App.toast(mensagem, tipo === 'erro' ? 'erro' : 'ok');
    }

    const formularioSenha = document.getElementById('formSenha');
    if (formularioSenha) {
        formularioSenha.addEventListener('submit', validarSenha);
    }
});
