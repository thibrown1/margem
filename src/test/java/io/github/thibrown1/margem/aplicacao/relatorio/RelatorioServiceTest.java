package io.github.thibrown1.margem.aplicacao.relatorio;

import io.github.thibrown1.margem.dominio.lucro.CalculadoraLucro;
import io.github.thibrown1.margem.dominio.lucro.ParametrosLoja;
import io.github.thibrown1.margem.dominio.pedido.ItemPedido;
import io.github.thibrown1.margem.dominio.pedido.OrigemPedido;
import io.github.thibrown1.margem.dominio.pedido.Pedido;
import io.github.thibrown1.margem.dominio.pedido.StatusPedido;
import io.github.thibrown1.margem.dominio.pedido.TaxasShopee;
import io.github.thibrown1.margem.suporte.PedidoRepositorioEmMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RelatorioServiceTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 9, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 9, 30);

    private final PedidoRepositorioEmMemoria pedidos = new PedidoRepositorioEmMemoria();
    private final RelatorioService relatorios = new RelatorioService(pedidos,
            new CalculadoraLucro(new ParametrosLoja(new BigDecimal("1.50"), new BigDecimal("4"))),
            Clock.fixed(Instant.parse("2026-09-29T15:00:00Z"), ZoneId.of("America/Sao_Paulo")));

    @BeforeEach
    void pedidosDoMes() {
        salvar("G1", StatusPedido.CONCLUIDO, "11.98", "4.00", item("GARRAFA-INOX-500", 1, "59.90", "22.40"));
        salvar("G2", StatusPedido.ENVIADO, "11.98", "4.00", item("GARRAFA-INOX-500", 1, "59.90", "22.40"));
        salvar("S1", StatusPedido.CONCLUIDO, "3.98", "4.00", item("SUPORTE-CELULAR", 1, "19.90", "13.90"));
        salvar("S2", StatusPedido.CONCLUIDO, "3.98", "4.00", item("SUPORTE-CELULAR", 1, "19.90", "13.90"));
        salvar("S3", StatusPedido.CONCLUIDO, "3.98", "4.00", item("SUPORTE-CELULAR", 1, "19.90", "13.90"));
        salvar("CANCELADO", StatusPedido.CANCELADO, "0", "0", item("GARRAFA-INOX-500", 1, "59.90", "22.40"));
    }

    @Test
    void resumoIgnoraCanceladosEExplicaParaOndeFoiODinheiro() {
        ResumoFinanceiro resumo = relatorios.resumo(INICIO, FIM);

        assertThat(resumo.pedidos()).isEqualTo(5);
        assertThat(resumo.pedidosCanceladosOuDevolvidos()).isEqualTo(1);
        assertThat(resumo.faturamentoBruto()).isEqualByComparingTo("179.50");
        assertThat(resumo.taxasShopee()).isEqualByComparingTo("55.90");
        // 2 garrafas com R$ 17,62 de lucro cada e 3 suportes com prejuízo de R$ 4,28 cada
        assertThat(resumo.lucroLiquido()).isEqualByComparingTo("22.40");
        assertThat(resumo.paraOndeFoiODinheiro().stream().map(ComposicaoItem::valor).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(resumo.faturamentoBruto());
        assertThat(resumo.avisos()).isEmpty();
    }

    @Test
    void oMaisVendidoNaoEhOQueMaisDaLucro() {
        List<LucroPorProduto> porQuantidade = relatorios.lucroPorProduto(INICIO, FIM, OrdenacaoProdutos.QUANTIDADE);
        List<LucroPorProduto> porLucro = relatorios.lucroPorProduto(INICIO, FIM, OrdenacaoProdutos.LUCRO);

        assertThat(porQuantidade.getFirst().sku()).isEqualTo("SUPORTE-CELULAR");
        assertThat(porLucro.getFirst().sku()).isEqualTo("GARRAFA-INOX-500");
        assertThat(porLucro.getFirst().lucroPorUnidade()).isEqualByComparingTo("17.62");
    }

    @Test
    void listaProdutosNoPrejuizo() {
        List<LucroPorProduto> prejuizo = relatorios.produtosNoPrejuizo(INICIO, FIM);

        assertThat(prejuizo).singleElement().satisfies(produto -> {
            assertThat(produto.sku()).isEqualTo("SUPORTE-CELULAR");
            assertThat(produto.lucro()).isEqualByComparingTo("-12.84");
        });
    }

    @Test
    void avisaQuandoHaCustoPendente() {
        salvar("C1", StatusPedido.CONCLUIDO, "13.98", "4.00", item("CAPA-NOTEBOOK-15", 1, "69.90", null));

        ResumoFinanceiro resumo = relatorios.resumo(INICIO, FIM);

        assertThat(resumo.avisos()).singleElement().asString().contains("CAPA-NOTEBOOK-15");
    }

    private void salvar(String codigo, StatusPedido status, String comissao, String servico, ItemPedido item) {
        pedidos.salvar(Pedido.novo(codigo, status, OrigemPedido.PLANILHA, Instant.parse("2026-09-15T15:00:00Z"), null,
                BigDecimal.ZERO, new TaxasShopee(new BigDecimal(comissao), new BigDecimal(servico), null), false, List.of(item)));
    }

    private static ItemPedido item(String sku, int quantidade, String preco, String custo) {
        return ItemPedido.novo(sku, sku, quantidade, new BigDecimal(preco), custo == null ? null : new BigDecimal(custo));
    }
}
