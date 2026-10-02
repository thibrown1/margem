package io.github.thibrown1.margem.aplicacao.produto;

import java.math.BigDecimal;

public record NovoProduto(String sku, String nome, BigDecimal custoUnitario, int estoqueInicial, int estoqueMinimo) {
}
