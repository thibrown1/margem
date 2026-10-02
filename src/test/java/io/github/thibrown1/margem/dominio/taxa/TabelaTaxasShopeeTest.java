package io.github.thibrown1.margem.dominio.taxa;

import io.github.thibrown1.margem.dominio.RegraDeNegocioException;
import io.github.thibrown1.margem.dominio.pedido.TaxasShopee;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static io.github.thibrown1.margem.dominio.taxa.TabelasDeTeste.faixa;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TabelaTaxasShopeeTest {

    private final TabelaTaxasShopee tabela = TabelasDeTeste.shopee2026();

    @ParameterizedTest
    @CsvSource({
            "7.99, 50, 0.00",
            "8.00, 20, 4.00",
            "79.99, 20, 4.00",
            "80.00, 14, 16.00",
            "99.99, 14, 16.00",
            "100.00, 14, 20.00",
            "250.00, 14, 26.00"})
    void escolheAFaixaPeloPrecoDoItem(String preco, String comissao, String taxaFixa) {
        FaixaTaxa faixa = tabela.faixaPara(new BigDecimal(preco));

        assertThat(faixa.comissaoPercentual()).isEqualByComparingTo(comissao);
        assertThat(faixa.taxaFixaPorItem()).isEqualByComparingTo(taxaFixa);
    }

    @Test
    void cobraComissaoSobreOValorETaxaFixaPorUnidade() {
        TaxasShopee taxas = tabela.taxasPara(new BigDecimal("59.90"), 2);

        assertThat(taxas.comissao()).isEqualByComparingTo("23.96");
        assertThat(taxas.servico()).isEqualByComparingTo("8.00");
        assertThat(taxas.total()).isEqualByComparingTo("31.96");
    }

    @Test
    void somaTaxaAdicionalDoVendedorCpf() {
        TabelaTaxasShopee comAdicional = new TabelaTaxasShopee(tabela.getFaixas(), new BigDecimal("3.00"));

        assertThat(comAdicional.taxasPara(new BigDecimal("59.90"), 1).servico()).isEqualByComparingTo("7.00");
    }

    @Test
    void informaOLimiteDeCadaFaixa() {
        assertThat(tabela.limiteSuperior(tabela.faixaPara(new BigDecimal("50")))).contains(new BigDecimal("80.00"));
        assertThat(tabela.limiteSuperior(tabela.faixaPara(new BigDecimal("500")))).isEmpty();
    }

    @Test
    void validaATabela() {
        assertThatThrownBy(() -> new TabelaTaxasShopee(List.of(faixa("8.00", "20", "4.00")), null))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("R$ 0,00");
        assertThatThrownBy(() -> new TabelaTaxasShopee(List.of(faixa("0", "20", "4"), faixa("0.00", "14", "16")), null))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> faixa("0", "100", "0")).isInstanceOf(RegraDeNegocioException.class);
    }
}
