package io.github.thibrown1.margem.adaptador.entrada.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record TabelaTaxasResponse(
        String vigencia,
        List<Faixa> faixas,
        BigDecimal taxaAdicionalPorItem,
        BigDecimal custoEmbalagemPorPedido,
        BigDecimal aliquotaImposto) {

    /** @param ate último preço da faixa; nulo na última faixa */
    public record Faixa(BigDecimal de, BigDecimal ate, BigDecimal comissaoPercentual, BigDecimal taxaFixaPorItem) {
    }
}
