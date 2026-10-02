package io.github.thibrown1.margem.dominio.taxa;

import io.github.thibrown1.margem.dominio.Moeda;
import io.github.thibrown1.margem.dominio.RegraDeNegocioException;

import java.math.BigDecimal;

/**
 * Uma faixa da tabela de comissões da Shopee.
 *
 * @param precoMinimo        preço do item a partir do qual a faixa vale (inclusive)
 * @param comissaoPercentual percentual cobrado sobre o preço do item
 * @param taxaFixaPorItem    valor fixo cobrado por unidade vendida
 */
public record FaixaTaxa(BigDecimal precoMinimo, BigDecimal comissaoPercentual, BigDecimal taxaFixaPorItem) {

    public FaixaTaxa {
        if (precoMinimo == null || precoMinimo.signum() < 0) {
            throw new RegraDeNegocioException("O preço mínimo da faixa não pode ser negativo");
        }
        if (comissaoPercentual == null || comissaoPercentual.signum() < 0
                || comissaoPercentual.compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw new RegraDeNegocioException("A comissão da faixa deve estar entre 0 e 100%");
        }
        if (taxaFixaPorItem == null || taxaFixaPorItem.signum() < 0) {
            throw new RegraDeNegocioException("A taxa fixa da faixa não pode ser negativa");
        }
        precoMinimo = Moeda.arredondar(precoMinimo);
        taxaFixaPorItem = Moeda.arredondar(taxaFixaPorItem);
    }
}
