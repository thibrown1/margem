-- Produtos de demonstração (perfil "demo"). Os preços de venda estão na Shopee simulada.
-- A "Capa para Notebook" fica de fora de propósito, para mostrar como o sistema trata SKU sem cadastro.

INSERT INTO produto (sku, nome, custo_unitario, estoque_atual, estoque_minimo, versao, criado_em, atualizado_em)
SELECT 'GARRAFA-INOX-500', 'Garrafa Térmica Inox 500ml', 22.40, 140, 10, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE sku = 'GARRAFA-INOX-500');

INSERT INTO produto (sku, nome, custo_unitario, estoque_atual, estoque_minimo, versao, criado_em, atualizado_em)
SELECT 'KIT-POTES-3', 'Kit 3 Potes Herméticos de Vidro', 24.00, 90, 8, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE sku = 'KIT-POTES-3');

INSERT INTO produto (sku, nome, custo_unitario, estoque_atual, estoque_minimo, versao, criado_em, atualizado_em)
SELECT 'ORGANIZADOR-GAVETA', 'Organizador de Gaveta 8 Divisórias', 12.80, 120, 15, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE sku = 'ORGANIZADOR-GAVETA');

INSERT INTO produto (sku, nome, custo_unitario, estoque_atual, estoque_minimo, versao, criado_em, atualizado_em)
SELECT 'SUPORTE-CELULAR', 'Suporte de Celular Articulado', 13.90, 100, 10, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE sku = 'SUPORTE-CELULAR');

INSERT INTO produto (sku, nome, custo_unitario, estoque_atual, estoque_minimo, versao, criado_em, atualizado_em)
SELECT 'LUMINARIA-LED', 'Luminária LED de Mesa Recarregável', 31.50, 30, 8, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE sku = 'LUMINARIA-LED');

INSERT INTO produto (sku, nome, custo_unitario, estoque_atual, estoque_minimo, versao, criado_em, atualizado_em)
SELECT 'CAFETEIRA-ITALIANA', 'Cafeteira Italiana 6 Xícaras', 58.00, 28, 5, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE sku = 'CAFETEIRA-ITALIANA');
