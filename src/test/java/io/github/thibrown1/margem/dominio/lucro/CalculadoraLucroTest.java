package io.github.thibrown1.margem.dominio.lucro;

import io.github.thibrown1.margem.dominio.pedido.ItemPedido;
import io.github.thibrown1.margem.dominio.pedido.OrigemPedido;
import io.github.thibrown1.margem.dominio.pedido.Pedido;
import io.github.thibrown1.margem.dominio.pedido.StatusPedido;
import io.github.thibrown1.margem.dominio.pedido.TaxasShopee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Os valores esperados foram calculados à mão, com embalagem de R$ 1,50 e imposto de 4%. */
class CalculadoraLucroTest {

    private final CalculadoraLucro calculadora = new CalculadoraLucro(
            new ParametrosLoja(new BigDecimal("1.50"), new BigDecimal("4")));

    @Test
    void calculaOLucroRealDeUmPedidoSimples() {
        Pedido pedido = pedido(BigDecimal.ZERO, new TaxasShopee(new BigDecimal("11.98"), new BigDecimal("4.00"), null),
                ItemPedido.novo("GARRAFA-INOX-500", "Garrafa", 1, new BigDecimal("59.90"), new BigDecimal("22.40")));

        LucroPedido lucro = calculadora.calcular(pedido);

        assertThat(lucro.valorBruto()).isEqualByComparingTo("59.90");
        assertThat(lucro.totalTaxasShopee()).isEqualByComparingTo("15.98");
        assertThat(lucro.valorRecebido()).isEqualByComparingTo("43.92");
        assertThat(lucro.impostos()).isEqualByComparingTo("2.40");
        assertThat(lucro.custoProdutos()).isEqualByComparingTo("22.40");
        assertThat(lucro.custoEmbalagem()).isEqualByComparingTo("1.50");
        assertThat(lucro.lucro()).isEqualByComparingTo("17.62");
        assertThat(lucro.margemPercentual()).isEqualByComparingTo("29.42");
        assertThat(lucro.custoPendente()).isFalse();
    }

    @Test
    void rateiaCupomTaxasEmbalagemEImpostoEntreOsItens() {
        Pedido pedido = pedido(new BigDecimal("5.00"), new TaxasShopee(new BigDecimal("21.94"), new BigDecimal("12.00"), null),
                ItemPedido.novo("GARRAFA-INOX-500", "Garrafa", 1, new BigDecimal("59.90"), new BigDecimal("22.40")),
                ItemPedido.novo("ORGANIZADOR-GAVETA", "Organizador", 2, new BigDecimal("24.90"), new BigDecimal("12.80")));

        LucroPedido lucro = calculadora.calcular(pedido);

        assertThat(lucro.valorBruto()).isEqualByComparingTo("109.70");
        assertThat(lucro.valorRecebido()).isEqualByComparingTo("70.76");
        assertThat(lucro.impostos()).isEqualByComparingTo("4.19");
        assertThat(lucro.custoProdutos()).isEqualByComparingTo("48.00");
        assertThat(lucro.lucro()).isEqualByComparingTo("17.07");
        assertThat(lucro.margemPercentual()).isEqualByComparingTo("16.30");

        LucroItem garrafa = lucro.itens().getFirst();
        assertThat(garrafa.descontoVendedor()).isEqualByComparingTo("2.73");
        assertThat(garrafa.taxasShopee()).isEqualByComparingTo("18.53");
        assertThat(garrafa.lucro()).isEqualByComparingTo("13.13");

        BigDecimal somaDosItens = lucro.itens().stream().map(LucroItem::lucro).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(somaDosItens).isEqualByComparingTo(lucro.lucro());
    }

    @Test
    void mostraQuandoUmProdutoEhVendidoNoPrejuizo() {
        Pedido pedido = pedido(BigDecimal.ZERO, new TaxasShopee(new BigDecimal("3.98"), new BigDecimal("4.00"), null),
                ItemPedido.novo("SUPORTE-CELULAR", "Suporte", 1, new BigDecimal("19.90"), new BigDecimal("13.90")));

        LucroPedido lucro = calculadora.calcular(pedido);

        assertThat(lucro.lucro()).isEqualByComparingTo("-4.28");
        assertThat(lucro.margemPercentual()).isNegative();
    }

    @Test
    void itemSemCustoCadastradoFicaMarcadoComoPendente() {
        Pedido pedido = pedido(BigDecimal.ZERO, TaxasShopee.NENHUMA,
                ItemPedido.novo("CAPA-NOTEBOOK-15", "Capa", 1, new BigDecimal("69.90"), null));

        LucroPedido lucro = calculadora.calcular(pedido);

        assertThat(lucro.custoPendente()).isTrue();
        assertThat(lucro.itens().getFirst().custoPendente()).isTrue();
        assertThat(lucro.custoProdutos()).isEqualByComparingTo("0");
    }

    private static Pedido pedido(BigDecimal cupom, TaxasShopee taxas, ItemPedido... itens) {
        return Pedido.novo("PEDIDO-1", StatusPedido.CONCLUIDO, OrigemPedido.PLANILHA, Instant.parse("2026-09-20T15:00:00Z"),
                null, cupom, taxas, false, List.of(itens));
    }
}
