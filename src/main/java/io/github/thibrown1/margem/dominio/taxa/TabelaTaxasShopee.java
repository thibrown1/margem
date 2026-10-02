package io.github.thibrown1.margem.dominio.taxa;

import io.github.thibrown1.margem.dominio.Moeda;
import io.github.thibrown1.margem.dominio.RegraDeNegocioException;
import io.github.thibrown1.margem.dominio.pedido.TaxasShopee;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Tabela de comissões da Shopee por faixa de preço do item.
 * A Shopee muda essas regras de tempos em tempos, então os valores vêm de configuração, não do código.
 * Cada faixa vale do seu preço mínimo até o preço mínimo da próxima (exclusive).
 */
public class TabelaTaxasShopee {

    private final List<FaixaTaxa> faixas;
    private final BigDecimal taxaAdicionalPorItem;

    public TabelaTaxasShopee(List<FaixaTaxa> faixas, BigDecimal taxaAdicionalPorItem) {
        if (faixas == null || faixas.isEmpty()) {
            throw new RegraDeNegocioException("A tabela de taxas precisa de pelo menos uma faixa");
        }
        List<FaixaTaxa> ordenadas = faixas.stream().sorted(Comparator.comparing(FaixaTaxa::precoMinimo)).toList();
        if (ordenadas.getFirst().precoMinimo().signum() != 0) {
            throw new RegraDeNegocioException("A primeira faixa de taxas deve começar em R$ 0,00");
        }
        for (int i = 1; i < ordenadas.size(); i++) {
            if (ordenadas.get(i).precoMinimo().compareTo(ordenadas.get(i - 1).precoMinimo()) == 0) {
                throw new RegraDeNegocioException("Duas faixas de taxas começam no mesmo preço: "
                        + Moeda.formatar(ordenadas.get(i).precoMinimo()));
            }
        }
        if (taxaAdicionalPorItem != null && taxaAdicionalPorItem.signum() < 0) {
            throw new RegraDeNegocioException("A taxa adicional por item não pode ser negativa");
        }
        this.faixas = ordenadas;
        this.taxaAdicionalPorItem = Moeda.ouZero(taxaAdicionalPorItem);
    }

    public FaixaTaxa faixaPara(BigDecimal precoUnitario) {
        FaixaTaxa escolhida = faixas.getFirst();
        for (FaixaTaxa faixa : faixas) {
            if (precoUnitario.compareTo(faixa.precoMinimo()) >= 0) {
                escolhida = faixa;
            }
        }
        return escolhida;
    }

    /** Preço a partir do qual a próxima faixa começa, se existir. */
    public Optional<BigDecimal> limiteSuperior(FaixaTaxa faixa) {
        int indice = faixas.indexOf(faixa);
        return indice >= 0 && indice < faixas.size() - 1
                ? Optional.of(faixas.get(indice + 1).precoMinimo())
                : Optional.empty();
    }

    /** Taxas que a Shopee cobra por vender {@code quantidade} unidades a {@code precoUnitario}. */
    public TaxasShopee taxasPara(BigDecimal precoUnitario, int quantidade) {
        FaixaTaxa faixa = faixaPara(precoUnitario);
        BigDecimal unidades = BigDecimal.valueOf(quantidade);
        BigDecimal comissao = Moeda.percentualDe(precoUnitario.multiply(unidades), faixa.comissaoPercentual());
        BigDecimal taxaFixa = faixa.taxaFixaPorItem().add(taxaAdicionalPorItem).multiply(unidades);
        return new TaxasShopee(comissao, taxaFixa, BigDecimal.ZERO);
    }

    public List<FaixaTaxa> getFaixas() {
        return faixas;
    }

    public BigDecimal getTaxaAdicionalPorItem() {
        return taxaAdicionalPorItem;
    }
}
