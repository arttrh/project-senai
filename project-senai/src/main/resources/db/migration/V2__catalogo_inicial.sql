-- Cardapio inicial para a aplicacao subir com conteudo utilizavel.
--
-- Nenhum usuario e criado aqui de proposito: senha em migration versionada
-- vira credencial publica no repositorio. O primeiro ADMIN e criado no boot a
-- partir de variaveis de ambiente (ver AdministradorInicialRunner).

INSERT INTO produto (nome_produto, descricao_produto, preco, categoria, produto_ativo) VALUES
    ('Coxinha',            'Massa crocante com recheio de frango desfiado', 7.50,  'LANCHE',    TRUE),
    ('Pao de Queijo',      'Porcao com 3 unidades quentinhas',              6.00,  'LANCHE',    TRUE),
    ('Misto Quente',       'Pao de forma, presunto e queijo na chapa',      9.00,  'LANCHE',    TRUE),
    ('Empada de Palmito',  'Massa amanteigada com recheio cremoso',         8.00,  'LANCHE',    TRUE),
    ('Refrigerante Lata',  'Lata 350ml gelada',                             5.50,  'BEBIDA',    TRUE),
    ('Suco Natural',       'Copo 300ml de laranja ou maracuja',             7.00,  'BEBIDA',    TRUE),
    ('Agua Mineral',       'Garrafa 500ml sem gas',                         3.50,  'BEBIDA',    TRUE),
    ('Cafe com Leite',     'Copo 200ml',                                    4.50,  'BEBIDA',    TRUE),
    ('Pudim',              'Fatia de pudim de leite condensado',            8.50,  'SOBREMESA', TRUE),
    ('Brigadeiro',         'Unidade tradicional',                           3.00,  'SOBREMESA', TRUE);

INSERT INTO estoque (id_produto, quantidade, versao)
SELECT id_produto, 25, 0 FROM produto;
