package io.github.thibrown1.margem.aplicacao.pedido;

import java.math.BigDecimal;

public record ItemImportado(String sku, String descricao, int quantidade, BigDecimal precoUnitario) {
}
