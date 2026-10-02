package io.github.thibrown1.margem.dominio.taxa;

import java.math.BigDecimal;
import java.util.List;

/** Tabela da Shopee Brasil vigente desde 01/03/2026, a mesma do application.yml. */
public final class TabelasDeTeste {

    private TabelasDeTeste() {
    }

    public static TabelaTaxasShopee shopee2026() {
        return new TabelaTaxasShopee(List.of(
                faixa("0.00", "50", "0.00"),
                faixa("8.00", "20", "4.00"),
                faixa("80.00", "14", "16.00"),
                faixa("100.00", "14", "20.00"),
                faixa("200.00", "14", "26.00")), BigDecimal.ZERO);
    }

    public static FaixaTaxa faixa(String minimo, String comissao, String fixa) {
        return new FaixaTaxa(new BigDecimal(minimo), new BigDecimal(comissao), new BigDecimal(fixa));
    }
}
