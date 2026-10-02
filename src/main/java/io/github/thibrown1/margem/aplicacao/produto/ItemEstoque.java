package io.github.thibrown1.margem.aplicacao.produto;

import io.github.thibrown1.margem.dominio.produto.SituacaoEstoque;

/** Visão de estoque sem valores financeiros, que a expedição também pode ver. */
public record ItemEstoque(String sku, String nome, int estoqueAtual, int estoqueMinimo, SituacaoEstoque situacao) {
}
