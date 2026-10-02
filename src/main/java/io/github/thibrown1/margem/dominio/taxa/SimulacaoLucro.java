package io.github.thibrown1.margem.dominio.taxa;

import java.math.BigDecimal;

/**
 * Resultado de uma simulação de venda de 1 unidade num pedido.
 *
 * @param valorRecebido o que a Shopee repassa: preço - comissão - taxa fixa
 * @param alerta        preenchido quando o preço cai numa faixa em que vender mais caro dá menos lucro
 */
public record SimulacaoLucro(
        BigDecimal precoVenda,
        BigDecimal comissaoPercentual,
        BigDecimal comissao,
        BigDecimal taxaFixa,
        BigDecimal valorRecebido,
        BigDecimal impostos,
        BigDecimal custoEmbalagem,
        BigDecimal custoProduto,
        BigDecimal lucro,
        BigDecimal margemPercentual,
        String alerta) {
}
