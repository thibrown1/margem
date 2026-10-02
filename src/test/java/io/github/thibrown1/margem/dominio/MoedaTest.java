package io.github.thibrown1.margem.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MoedaTest {

    @Test
    void formataNoPadraoBrasileiro() {
        assertThat(Moeda.formatar(new BigDecimal("1234.5"))).isEqualTo("R$ 1.234,50");
        assertThat(Moeda.formatar(new BigDecimal("-4.28"))).isEqualTo("-R$ 4,28");
        assertThat(Moeda.formatar(BigDecimal.ZERO)).isEqualTo("R$ 0,00");
    }

    @Test
    void aplicaPercentualArredondandoMeioCentavoParaCima() {
        assertThat(Moeda.percentualDe(new BigDecimal("59.90"), BigDecimal.valueOf(20))).isEqualByComparingTo("11.98");
        assertThat(Moeda.percentualDe(new BigDecimal("7.99"), BigDecimal.valueOf(50))).isEqualByComparingTo("4.00");
    }

    @Test
    void participacaoSobreTotalZeroEhZero() {
        assertThat(Moeda.participacao(BigDecimal.TEN, BigDecimal.ZERO)).isEqualByComparingTo("0");
        assertThat(Moeda.participacao(new BigDecimal("17.62"), new BigDecimal("59.90"))).isEqualByComparingTo("29.42");
    }

    @Test
    void somaComDuasCasas() {
        assertThat(Moeda.somar(List.of(new BigDecimal("0.1"), new BigDecimal("0.2")))).isEqualTo(new BigDecimal("0.30"));
    }
}
