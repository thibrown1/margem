package io.github.thibrown1.margem.configuracao;

import io.github.thibrown1.margem.aplicacao.alerta.ParametrosAlertas;
import io.github.thibrown1.margem.dominio.lucro.CalculadoraLucro;
import io.github.thibrown1.margem.dominio.lucro.ParametrosLoja;
import io.github.thibrown1.margem.dominio.taxa.FaixaTaxa;
import io.github.thibrown1.margem.dominio.taxa.SimuladorPreco;
import io.github.thibrown1.margem.dominio.taxa.TabelaTaxasShopee;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

/**
 * Monta os objetos de domínio a partir da configuração. O domínio não conhece o Spring:
 * é aqui que as regras puras viram beans.
 */
@Configuration
public class BeansConfig {

    @Bean
    public Clock relogio(MargemProperties propriedades) {
        return Clock.system(ZoneId.of(propriedades.fusoHorario()));
    }

    @Bean
    public TabelaTaxasShopee tabelaTaxasShopee(MargemProperties propriedades) {
        MargemProperties.TabelaTaxas tabela = propriedades.tabelaTaxas();
        return new TabelaTaxasShopee(
                tabela.faixas().stream()
                        .map(f -> new FaixaTaxa(f.precoMinimo(), f.comissaoPercentual(), f.taxaFixa()))
                        .toList(),
                tabela.taxaAdicionalPorItem());
    }

    @Bean
    public ParametrosLoja parametrosLoja(MargemProperties propriedades) {
        return new ParametrosLoja(propriedades.loja().custoEmbalagemPorPedido(), propriedades.loja().aliquotaImposto());
    }

    @Bean
    public CalculadoraLucro calculadoraLucro(ParametrosLoja parametrosLoja) {
        return new CalculadoraLucro(parametrosLoja);
    }

    @Bean
    public SimuladorPreco simuladorPreco(TabelaTaxasShopee tabela, ParametrosLoja parametrosLoja) {
        return new SimuladorPreco(tabela, parametrosLoja);
    }

    @Bean
    public ParametrosAlertas parametrosAlertas(MargemProperties propriedades) {
        MargemProperties.Alertas alertas = propriedades.alertas();
        return new ParametrosAlertas(
                Duration.ofHours(alertas.horasAntesDoPrazo()),
                Duration.ofHours(alertas.reenviarAposHoras()),
                Duration.ofDays(alertas.diasVerificarPrejuizo()));
    }
}
