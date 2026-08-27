-- Schema inicial da Cantina SENAI.
--
-- Substitui o antigo src/database/mySql.sql, que comecava com DROP DATABASE e
-- por isso nao podia ser aplicado em nenhum ambiente ja em uso.
--
-- SQL mantido portavel entre MySQL (producao) e H2 em modo MySQL (testes):
-- sem ENGINE=, sem tipos exclusivos de um dos dois bancos.

CREATE TABLE usuario (
    id_usuario   BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome         VARCHAR(120) NOT NULL,
    cpf          VARCHAR(14)  NOT NULL,
    telefone     VARCHAR(20),
    email        VARCHAR(180) NOT NULL,
    senha        VARCHAR(100) NOT NULL,
    tipo_usuario VARCHAR(20)  NOT NULL,
    ativo        BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_usuario_cpf   UNIQUE (cpf),
    CONSTRAINT uk_usuario_email UNIQUE (email)
);

CREATE TABLE produto (
    id_produto        BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome_produto      VARCHAR(120)   NOT NULL,
    descricao_produto VARCHAR(255),
    preco             DECIMAL(10, 2) NOT NULL,
    categoria         VARCHAR(20)    NOT NULL,
    produto_ativo     BOOLEAN        NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_produto_nome UNIQUE (nome_produto),
    CONSTRAINT ck_produto_preco CHECK (preco >= 0)
);

-- UNIQUE em id_produto: o codigo antigo permitia varias linhas de estoque para
-- o mesmo produto, o que fazia findByProduto_IdProduto quebrar com
-- NonUniqueResultException. Agora a relacao e 1:1 no banco.
CREATE TABLE estoque (
    id_estoque BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_produto BIGINT NOT NULL,
    quantidade INT    NOT NULL DEFAULT 0,
    versao     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_estoque_produto UNIQUE (id_produto),
    CONSTRAINT fk_estoque_produto FOREIGN KEY (id_produto) REFERENCES produto (id_produto),
    CONSTRAINT ck_estoque_quantidade CHECK (quantidade >= 0)
);

CREATE TABLE pedido (
    id_pedido       BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario      BIGINT         NOT NULL,
    data_pedido     DATETIME       NOT NULL,
    status_pedido   VARCHAR(20)    NOT NULL,
    forma_pagamento VARCHAR(20)    NOT NULL,
    observacao      VARCHAR(255),
    valor_total     DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_pedido_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);

CREATE TABLE item_pedido (
    id_item_pedido BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_pedido      BIGINT         NOT NULL,
    id_produto     BIGINT         NOT NULL,
    quantidade     INT            NOT NULL,
    preco_unitario DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_item_pedido_pedido  FOREIGN KEY (id_pedido)  REFERENCES pedido (id_pedido),
    CONSTRAINT fk_item_pedido_produto FOREIGN KEY (id_produto) REFERENCES produto (id_produto),
    CONSTRAINT ck_item_pedido_quantidade CHECK (quantidade > 0)
);

CREATE INDEX idx_pedido_usuario ON pedido (id_usuario);
CREATE INDEX idx_pedido_status  ON pedido (status_pedido);
CREATE INDEX idx_item_pedido_pedido ON item_pedido (id_pedido);
