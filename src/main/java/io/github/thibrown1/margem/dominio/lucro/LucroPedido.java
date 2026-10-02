package io.github.thibrown1.margem.dominio.lucro;

import java.math.BigDecimal;
import java.util.List;

/**
 * A conta completa de um pedido: do preço que o cliente pagou até o que sobra de verdade.
 *
 * <pre>
 * valorBruto - descontoVendedor - taxas da Shopee          = valorRecebido (o que cai na conta)
 * valorRecebido - custo dos produtos - embalagem - imposto = lucro
 * </pre>
 */
public record LucroPedido(
        String codigoPedido,
        BigDecimal valorBruto,
        BigDecimal descontoVendedor,
        BigDecimal taxaComissao,
        BigDecimal taxaServico,
        BigDecimal taxaTransacao,
        BigDecimal totalTaxasShopee,
        boolean taxasEstimadas,
        BigDecimal valorRecebido,
        BigDecimal custoProdutos,
        BigDecimal custoEmbalagem,
        BigDecimal impostos,
        BigDecimal lucro,
        BigDecimal margemPercentual,
        boolean custoPendente,
        List<LucroItem> itens) {
}
