package io.github.thibrown1.margem.aplicacao.pedido;

import io.github.thibrown1.margem.dominio.pedido.OrigemPedido;
import io.github.thibrown1.margem.dominio.pedido.Pedido;
import io.github.thibrown1.margem.dominio.pedido.StatusPedido;
import io.github.thibrown1.margem.dominio.pedido.TaxasShopee;
import io.github.thibrown1.margem.dominio.produto.Produto;
import io.github.thibrown1.margem.dominio.taxa.TabelasDeTeste;
import io.github.thibrown1.margem.suporte.PedidoRepositorioEmMemoria;
import io.github.thibrown1.margem.suporte.ProdutoRepositorioEmMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImportacaoPedidosServiceTest {

    private static final TaxasShopee TAXAS_REAIS = new TaxasShopee(new BigDecimal("25.00"), new BigDecimal("8.00"), null);

    private final PedidoRepositorioEmMemoria pedidos = new PedidoRepositorioEmMemoria();
    private final ProdutoRepositorioEmMemoria produtos = new ProdutoRepositorioEmMemoria();
    private final ImportacaoPedidosService importacao =
            new ImportacaoPedidosService(pedidos, produtos, TabelasDeTeste.shopee2026());

    @BeforeEach
    void cadastrarProduto() {
        produtos.cadastrar(Produto.novo("GARRAFA-INOX-500", "Garrafa", new BigDecimal("22.40"), 10, 3));
    }

    @Test
    void pedidoNovoBaixaOEstoqueECongelaOCusto() {
        ResultadoImportacao resultado = importar(pedido("P1", StatusPedido.AGUARDANDO_ENVIO, TAXAS_REAIS, "GARRAFA-INOX-500", 2));

        assertThat(resultado.pedidosNovos()).isEqualTo(1);
        assertThat(estoqueDaGarrafa()).isEqualTo(8);
        Pedido salvo = pedidos.buscarPorCodigo("P1").orElseThrow();
        assertThat(salvo.getItens().getFirst().getCustoUnitario()).isEqualByComparingTo("22.40");
        assertThat(salvo.isTaxasEstimadas()).isFalse();
    }

    @Test
    void importarDeNovoNaoDuplicaNemBaixaEstoqueDuasVezes() {
        importar(pedido("P1", StatusPedido.AGUARDANDO_ENVIO, TAXAS_REAIS, "GARRAFA-INOX-500", 2));

        ResultadoImportacao segunda = importar(pedido("P1", StatusPedido.AGUARDANDO_ENVIO, TAXAS_REAIS, "GARRAFA-INOX-500", 2));

        assertThat(segunda.pedidosNovos()).isZero();
        assertThat(segunda.pedidosSemAlteracao()).isEqualTo(1);
        assertThat(pedidos.contar()).isEqualTo(1);
        assertThat(estoqueDaGarrafa()).isEqualTo(8);
    }

    @Test
    void cancelamentoDevolveOEstoque() {
        importar(pedido("P1", StatusPedido.AGUARDANDO_ENVIO, TAXAS_REAIS, "GARRAFA-INOX-500", 2));

        ResultadoImportacao resultado = importar(pedido("P1", StatusPedido.CANCELADO, TaxasShopee.NENHUMA, "GARRAFA-INOX-500", 2));

        assertThat(resultado.pedidosAtualizados()).isEqualTo(1);
        assertThat(estoqueDaGarrafa()).isEqualTo(10);
        assertThat(pedidos.buscarPorCodigo("P1").orElseThrow().getStatus()).isEqualTo(StatusPedido.CANCELADO);
    }

    @Test
    void pedidoNaoPagoSoBaixaEstoqueQuandoForPago() {
        importar(pedido("P1", StatusPedido.NAO_PAGO, TAXAS_REAIS, "GARRAFA-INOX-500", 1));
        assertThat(estoqueDaGarrafa()).isEqualTo(10);

        importar(pedido("P1", StatusPedido.AGUARDANDO_ENVIO, TAXAS_REAIS, "GARRAFA-INOX-500", 1));
        assertThat(estoqueDaGarrafa()).isEqualTo(9);
    }

    @Test
    void skuSemCadastroFicaComCustoPendenteEAviso() {
        ResultadoImportacao resultado = importar(pedido("P1", StatusPedido.CONCLUIDO, TAXAS_REAIS, "capa-notebook-15", 1));

        assertThat(resultado.skusSemCadastro()).containsExactly("CAPA-NOTEBOOK-15");
        assertThat(resultado.avisos()).anyMatch(aviso -> aviso.contains("não cadastrado"));
        assertThat(pedidos.buscarPorCodigo("P1").orElseThrow().temCustoPendente()).isTrue();
        assertThat(estoqueDaGarrafa()).isEqualTo(10);
    }

    @Test
    void semTaxasNaOrigemUsaATabelaParaEstimar() {
        ResultadoImportacao resultado = importar(pedido("P1", StatusPedido.CONCLUIDO, null, "GARRAFA-INOX-500", 1));

        Pedido salvo = pedidos.buscarPorCodigo("P1").orElseThrow();
        assertThat(salvo.isTaxasEstimadas()).isTrue();
        assertThat(salvo.getTaxas().comissao()).isEqualByComparingTo("11.98");
        assertThat(salvo.getTaxas().servico()).isEqualByComparingTo("4.00");
        assertThat(resultado.avisos()).anyMatch(aviso -> aviso.contains("estimar"));
    }

    @Test
    void taxasEstimadasNaoSobrescrevemTaxasReais() {
        importar(pedido("P1", StatusPedido.ENVIADO, TAXAS_REAIS, "GARRAFA-INOX-500", 2));

        importar(pedido("P1", StatusPedido.CONCLUIDO, null, "GARRAFA-INOX-500", 2));

        Pedido salvo = pedidos.buscarPorCodigo("P1").orElseThrow();
        assertThat(salvo.getStatus()).isEqualTo(StatusPedido.CONCLUIDO);
        assertThat(salvo.getTaxas()).isEqualTo(TAXAS_REAIS);
        assertThat(salvo.isTaxasEstimadas()).isFalse();
    }

    @Test
    void pedidoInvalidoViraErroSemImpedirOsOutros() {
        ResultadoImportacao resultado = importar(
                pedido("RUIM", StatusPedido.CONCLUIDO, TAXAS_REAIS, "GARRAFA-INOX-500", 0),
                pedido("BOM", StatusPedido.CONCLUIDO, TAXAS_REAIS, "GARRAFA-INOX-500", 1));

        assertThat(resultado.pedidosNovos()).isEqualTo(1);
        assertThat(resultado.erros()).singleElement().satisfies(erro -> {
            assertThat(erro.pedido()).isEqualTo("RUIM");
            assertThat(erro.motivo()).contains("quantidade");
        });
    }

    private ResultadoImportacao importar(PedidoImportado... importados) {
        return importacao.importar(List.of(importados), OrigemPedido.PLANILHA);
    }

    private int estoqueDaGarrafa() {
        return produtos.buscarPorSku("GARRAFA-INOX-500").orElseThrow().getEstoqueAtual();
    }

    private static PedidoImportado pedido(String codigo, StatusPedido status, TaxasShopee taxas, String sku, int quantidade) {
        return new PedidoImportado(codigo, status, Instant.parse("2026-09-20T15:00:00Z"), Instant.parse("2026-09-22T15:00:00Z"),
                BigDecimal.ZERO, taxas, List.of(new ItemImportado(sku, "Produto " + sku, quantidade, new BigDecimal("59.90"))));
    }
}
