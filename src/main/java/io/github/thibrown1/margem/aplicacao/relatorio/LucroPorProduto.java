package io.github.thibrown1.margem.aplicacao.relatorio;

import java.math.BigDecimal;

public record LucroPorProduto(
        String sku,
        String descricao,
        int quantidadeVendida,
        BigDecimal faturamento,
        BigDecimal taxasShopee,
        BigDecimal custoProdutos,
        BigDecimal lucro,
        BigDecimal margemPercentual,
        BigDecimal lucroPorUnidade,
        boolean custoPendente) {
}
