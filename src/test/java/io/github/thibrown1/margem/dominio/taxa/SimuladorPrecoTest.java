package io.github.thibrown1.margem.dominio.taxa;

import io.github.thibrown1.margem.dominio.RegraDeNegocioException;
import io.github.thibrown1.margem.dominio.lucro.ParametrosLoja;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Embalagem de R$ 1,50 e imposto de 4%, com a tabela da Shopee de 2026. */
class SimuladorPrecoTest {

    private final SimuladorPreco simulador = new SimuladorPreco(TabelasDeTeste.shopee2026(),
            new ParametrosLoja(new BigDecimal("1.50"), new BigDecimal("4")));

    @Test
    void simulaQuantoSobraDeUmaVenda() {
        SimulacaoLucro simulacao = simulador.simularLucro(new BigDecimal("84.90"), new BigDecimal("31.50"));

        assertThat(simulacao.comissao()).isEqualByComparingTo("11.89");
        assertThat(simulacao.taxaFixa()).isEqualByComparingTo("16.00");
        assertThat(simulacao.valorRecebido()).isEqualByComparingTo("57.01");
        assertThat(simulacao.impostos()).isEqualByComparingTo("3.40");
        assertThat(simulacao.lucro()).isEqualByComparingTo("20.61");
        assertThat(simulacao.margemPercentual()).isEqualByComparingTo("24.28");
    }

    @Test
    void vendendoMaisCaroLogoDepoisDeR$80VoceGanhaMenos() {
        SimulacaoLucro aR$8490 = simulador.simularLucro(new BigDecimal("84.90"), new BigDecimal("31.50"));
        SimulacaoLucro aR$7999 = simulador.simularLucro(new BigDecimal("79.99"), new BigDecimal("31.50"));

        assertThat(aR$7999.lucro()).isEqualByComparingTo("23.79");
        assertThat(aR$7999.lucro()).isGreaterThan(aR$8490.lucro());
        assertThat(aR$8490.alerta()).contains("R$ 79,99").contains("de R$ 4,00 para R$ 16,00");
        assertThat(aR$7999.alerta()).isNull();
    }

    @Test
    void encontraAsFaixasDePrecoAEvitar() {
        List<FaixaDePrecoAEvitar> faixas = simulador.faixasAEvitar();

        assertThat(faixas).hasSize(4);
        assertThat(faixas.get(0).de()).isEqualByComparingTo("8.00");
        assertThat(faixas.get(0).ate()).isEqualByComparingTo("10.09");
        assertThat(faixas.get(1).de()).isEqualByComparingTo("80.00");
        assertThat(faixas.get(1).ate()).isEqualByComparingTo("88.77");
        assertThat(faixas.get(1).precoMelhor()).isEqualByComparingTo("79.99");

        for (FaixaDePrecoAEvitar faixa : faixas) {
            BigDecimal custo = new BigDecimal("30.00");
            BigDecimal lucroNoPrecoMelhor = simulador.simularLucro(faixa.precoMelhor(), custo).lucro();
            assertThat(simulador.simularLucro(faixa.ate(), custo).lucro()).isLessThanOrEqualTo(lucroNoPrecoMelhor);
            assertThat(simulador.simularLucro(faixa.ate().add(new BigDecimal("0.01")), custo).lucro()).isGreaterThan(lucroNoPrecoMelhor);
        }
    }

    @Test
    void calculaOMenorPrecoParaAtingirAMargem() {
        SimulacaoLucro simulacao = simulador.precoIdeal(new BigDecimal("22.40"), new BigDecimal("20"));

        // A R$ 49,82 sobram R$ 9,97 (20,01%). Um centavo a menos, sobram R$ 9,96, abaixo dos 20% de R$ 49,81.
        assertThat(simulacao.precoVenda()).isEqualByComparingTo("49.82");
        assertThat(simulacao.lucro()).isEqualByComparingTo("9.97");
        assertThat(simulador.simularLucro(new BigDecimal("49.81"), new BigDecimal("22.40")).lucro())
                .isLessThan(new BigDecimal("9.962"));
    }

    @Test
    void avisaQuandoAMargemEhImpossivel() {
        assertThatThrownBy(() -> simulador.precoIdeal(new BigDecimal("22.40"), new BigDecimal("90")))
                .isInstanceOf(MargemInviavelException.class)
                .hasMessageContaining("90%");
    }

    @Test
    void validaEntradas() {
        assertThatThrownBy(() -> simulador.simularLucro(BigDecimal.ZERO, BigDecimal.ONE)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> simulador.precoIdeal(BigDecimal.ONE, new BigDecimal("100"))).isInstanceOf(RegraDeNegocioException.class);
    }
}
