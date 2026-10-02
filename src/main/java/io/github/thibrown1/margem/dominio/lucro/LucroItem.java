package io.github.thibrown1.margem.dominio.lucro;

import java.math.BigDecimal;

/** Parte do lucro de um pedido que cabe a um item, com os custos do pedido já rateados. */
public record LucroItem(
        String sku,
        String descricao,
        int quantidade,
        BigDecimal receita,
        BigDecimal descontoVendedor,
        BigDecimal taxasShopee,
        BigDecimal custoProdutos,
        BigDecimal custoEmbalagem,
        BigDecimal impostos,
        BigDecimal lucro,
        boolean custoPendente) {
}
