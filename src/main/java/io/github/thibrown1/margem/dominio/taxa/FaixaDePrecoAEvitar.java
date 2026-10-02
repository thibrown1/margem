package io.github.thibrown1.margem.dominio.taxa;

import java.math.BigDecimal;

/**
 * Intervalo de preço em que subir o preço faz o vendedor ganhar menos, porque o item muda de faixa
 * e a taxa da Shopee sobe mais do que o aumento de preço.
 *
 * @param de          primeiro preço ruim (início da faixa mais cara)
 * @param ate         último preço ruim
 * @param precoMelhor preço logo abaixo da faixa, que rende mais do que qualquer preço do intervalo
 */
public record FaixaDePrecoAEvitar(BigDecimal de, BigDecimal ate, BigDecimal precoMelhor, String motivo) {

    public boolean contem(BigDecimal preco) {
        return preco.compareTo(de) >= 0 && preco.compareTo(ate) <= 0;
    }
}
