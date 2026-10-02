package io.github.thibrown1.margem.aplicacao.relatorio;

import java.math.BigDecimal;

/** Uma fatia do faturamento: para onde foi cada real vendido. */
public record ComposicaoItem(String destino, BigDecimal valor, BigDecimal percentual) {
}
