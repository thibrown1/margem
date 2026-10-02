package io.github.thibrown1.margem.dominio.lucro;

import io.github.thibrown1.margem.dominio.Moeda;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Divide um valor do pedido (taxas, cupom, embalagem) entre os itens, proporcional ao valor de cada item.
 * O centavo que sobra do arredondamento vai para o último item, então a soma das partes é sempre
 * exatamente igual ao total. Sem isso o relatório por produto não "fecha" com o resumo.
 */
public final class Rateio {

    private Rateio() {
    }

    public static List<BigDecimal> proporcional(BigDecimal total, List<BigDecimal> pesos) {
        if (pesos.isEmpty()) {
            return List.of();
        }
        BigDecimal totalArredondado = Moeda.arredondar(total);
        BigDecimal somaDosPesos = pesos.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        if (somaDosPesos.signum() == 0) {
            List<BigDecimal> iguais = new ArrayList<>(Collections.nCopies(pesos.size(), BigDecimal.ONE));
            return proporcional(totalArredondado, iguais);
        }

        List<BigDecimal> partes = new ArrayList<>(pesos.size());
        BigDecimal distribuido = BigDecimal.ZERO;
        for (int i = 0; i < pesos.size() - 1; i++) {
            BigDecimal parte = totalArredondado.multiply(pesos.get(i))
                    .divide(somaDosPesos, Moeda.ESCALA, Moeda.ARREDONDAMENTO);
            partes.add(parte);
            distribuido = distribuido.add(parte);
        }
        partes.add(Moeda.arredondar(totalArredondado.subtract(distribuido)));
        return partes;
    }
}
