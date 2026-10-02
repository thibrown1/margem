package io.github.thibrown1.margem.aplicacao.relatorio;

import java.util.Comparator;

public enum OrdenacaoProdutos {
    LUCRO(Comparator.comparing(LucroPorProduto::lucro).reversed()),
    QUANTIDADE(Comparator.comparingInt(LucroPorProduto::quantidadeVendida).reversed()),
    FATURAMENTO(Comparator.comparing(LucroPorProduto::faturamento).reversed()),
    MARGEM(Comparator.comparing(LucroPorProduto::margemPercentual).reversed());

    private final Comparator<LucroPorProduto> comparador;

    OrdenacaoProdutos(Comparator<LucroPorProduto> comparador) {
        this.comparador = comparador;
    }

    public Comparator<LucroPorProduto> comparador() {
        return comparador.thenComparing(LucroPorProduto::sku);
    }
}
