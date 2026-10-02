package io.github.thibrown1.margem.dominio.pedido;

import io.github.thibrown1.margem.dominio.Moeda;
import io.github.thibrown1.margem.dominio.RegraDeNegocioException;

import java.math.BigDecimal;

/**
 * Taxas que a Shopee desconta de um pedido. A Shopee às vezes exporta taxas como valores negativos,
 * por isso o sinal é ignorado: aqui toda taxa é um valor positivo que sai do bolso do vendedor.
 */
public record TaxasShopee(BigDecimal comissao, BigDecimal servico, BigDecimal transacao) {

    public static final TaxasShopee NENHUMA = new TaxasShopee(Moeda.ZERO, Moeda.ZERO, Moeda.ZERO);

    public TaxasShopee {
        comissao = normalizar(comissao);
        servico = normalizar(servico);
        transacao = normalizar(transacao);
    }

    public BigDecimal total() {
        return comissao.add(servico).add(transacao);
    }

    public TaxasShopee somar(TaxasShopee outras) {
        return new TaxasShopee(comissao.add(outras.comissao), servico.add(outras.servico), transacao.add(outras.transacao));
    }

    private static BigDecimal normalizar(BigDecimal valor) {
        if (valor == null) {
            return Moeda.ZERO;
        }
        BigDecimal positivo = Moeda.arredondar(valor.abs());
        if (positivo.compareTo(new BigDecimal("1000000")) >= 0) {
            throw new RegraDeNegocioException("Valor de taxa fora do esperado: " + valor);
        }
        return positivo;
    }
}
