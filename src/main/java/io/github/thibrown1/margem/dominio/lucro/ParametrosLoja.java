package io.github.thibrown1.margem.dominio.lucro;

import io.github.thibrown1.margem.dominio.Moeda;
import io.github.thibrown1.margem.dominio.RegraDeNegocioException;

import java.math.BigDecimal;

/**
 * Custos da loja que não vêm da Shopee.
 *
 * @param custoEmbalagemPorPedido caixa, fita, etiqueta... cobrado uma vez por pedido
 * @param aliquotaImposto         percentual de imposto sobre a venda (ex.: 4 para 4% do Simples Nacional; 0 para MEI)
 */
public record ParametrosLoja(BigDecimal custoEmbalagemPorPedido, BigDecimal aliquotaImposto) {

    public ParametrosLoja {
        if (custoEmbalagemPorPedido == null || custoEmbalagemPorPedido.signum() < 0) {
            throw new RegraDeNegocioException("O custo de embalagem não pode ser negativo");
        }
        if (aliquotaImposto == null || aliquotaImposto.signum() < 0 || aliquotaImposto.compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw new RegraDeNegocioException("A alíquota de imposto deve estar entre 0 e 100");
        }
        custoEmbalagemPorPedido = Moeda.arredondar(custoEmbalagemPorPedido);
    }
}
