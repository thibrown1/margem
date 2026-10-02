package io.github.thibrown1.margem.aplicacao.produto;

import io.github.thibrown1.margem.dominio.produto.Produto;

/**
 * @param itensDePedidosAtualizados itens vendidos antes do cadastro que ganharam o custo agora
 */
public record ProdutoCadastrado(Produto produto, int itensDePedidosAtualizados) {
}
