package io.github.thibrown1.margem.adaptador.entrada.web.dto;

import io.github.thibrown1.margem.dominio.produto.Produto;
import io.github.thibrown1.margem.dominio.produto.SituacaoEstoque;

import java.math.BigDecimal;

public record ProdutoResponse(String sku, String nome, BigDecimal custoUnitario, int estoqueAtual, int estoqueMinimo,
                              SituacaoEstoque situacaoEstoque) {

    public static ProdutoResponse de(Produto produto) {
        return new ProdutoResponse(produto.getSku(), produto.getNome(), produto.getCustoUnitario(),
                produto.getEstoqueAtual(), produto.getEstoqueMinimo(), produto.situacaoEstoque());
    }
}
