package io.github.thibrown1.margem.dominio.lucro;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RateioTest {

    @Test
    void centavoQueSobraVaiParaOUltimo() {
        List<BigDecimal> partes = Rateio.proporcional(new BigDecimal("10.00"), List.of(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));

        assertThat(partes).containsExactly(new BigDecimal("3.33"), new BigDecimal("3.33"), new BigDecimal("3.34"));
    }

    @Test
    void divideProporcionalAoValorDeCadaItem() {
        List<BigDecimal> partes = Rateio.proporcional(new BigDecimal("5.00"),
                List.of(new BigDecimal("59.90"), new BigDecimal("49.80")));

        assertThat(partes).containsExactly(new BigDecimal("2.73"), new BigDecimal("2.27"));
    }

    @Test
    void semPesoDivideIgualmente() {
        List<BigDecimal> partes = Rateio.proporcional(new BigDecimal("1.00"), List.of(BigDecimal.ZERO, BigDecimal.ZERO));

        assertThat(partes).containsExactly(new BigDecimal("0.50"), new BigDecimal("0.50"));
    }

    @Test
    void somaDasPartesEhSempreIgualAoTotal() {
        List<BigDecimal> pesos = List.of(new BigDecimal("19.90"), new BigDecimal("84.90"), new BigDecimal("7.77"),
                new BigDecimal("129.90"), new BigDecimal("0.01"));
        for (String total : List.of("0.01", "0.07", "33.94", "1.50", "999.99")) {
            BigDecimal soma = Rateio.proporcional(new BigDecimal(total), pesos).stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(soma).isEqualByComparingTo(total);
        }
    }

    @Test
    void semItensNaoHaRateio() {
        assertThat(Rateio.proporcional(BigDecimal.TEN, List.of())).isEmpty();
    }
}
