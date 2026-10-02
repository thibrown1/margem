package io.github.thibrown1.margem.adaptador.saida.shopee;

import io.github.thibrown1.margem.aplicacao.pedido.ItemImportado;
import io.github.thibrown1.margem.aplicacao.pedido.PedidoImportado;
import io.github.thibrown1.margem.dominio.pedido.StatusPedido;
import io.github.thibrown1.margem.dominio.pedido.TaxasShopee;
import io.github.thibrown1.margem.dominio.taxa.TabelaTaxasShopee;
import io.github.thibrown1.margem.dominio.taxa.TabelasDeTeste;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShopeeSimuladaGatewayTest {

    private static final Instant AGORA = Instant.parse("2026-09-29T15:00:00Z");
    private final TabelaTaxasShopee tabela = TabelasDeTeste.shopee2026();
    private final ShopeeSimuladaGateway shopee =
            new ShopeeSimuladaGateway(tabela, Clock.fixed(AGORA, ZoneId.of("America/Sao_Paulo")));

    @Test
    void mesmoPeriodoGeraSempreOsMesmosPedidos() {
        List<PedidoImportado> primeira = shopee.buscarPedidos(AGORA.minus(Duration.ofDays(10)), AGORA);
        List<PedidoImportado> segunda = shopee.buscarPedidos(AGORA.minus(Duration.ofDays(10)), AGORA);

        assertThat(primeira).isNotEmpty().isEqualTo(segunda);
        assertThat(primeira).extracting(PedidoImportado::codigo).doesNotHaveDuplicates();
    }

    @Test
    void nuncaGeraPedidoNoFuturoNemForaDoPeriodo() {
        Instant de = AGORA.minus(Duration.ofDays(5));

        assertThat(shopee.buscarPedidos(de, AGORA.plus(Duration.ofDays(3))))
                .allSatisfy(p -> assertThat(p.criadoEm()).isBetween(de, AGORA));
    }

    @Test
    void statusAcompanhaAIdadeDoPedido() {
        List<PedidoImportado> pedidos = shopee.buscarPedidos(AGORA.minus(Duration.ofDays(30)), AGORA);

        assertThat(pedidos).filteredOn(p -> p.criadoEm().isBefore(AGORA.minus(Duration.ofDays(4))))
                .extracting(PedidoImportado::status)
                .isNotEmpty()
                .isSubsetOf(StatusPedido.CONCLUIDO, StatusPedido.CANCELADO);
        assertThat(pedidos).filteredOn(p -> p.criadoEm().isAfter(AGORA.minus(Duration.ofHours(36))))
                .extracting(PedidoImportado::status)
                .isNotEmpty()
                .isSubsetOf(StatusPedido.AGUARDANDO_ENVIO, StatusPedido.CANCELADO);
    }

    @Test
    void taxasSeguemATabelaConfigurada() {
        shopee.buscarPedidos(AGORA.minus(Duration.ofDays(15)), AGORA).stream()
                .filter(p -> p.status() != StatusPedido.CANCELADO)
                .forEach(pedido -> {
                    TaxasShopee esperadas = TaxasShopee.NENHUMA;
                    for (ItemImportado item : pedido.itens()) {
                        esperadas = esperadas.somar(tabela.taxasPara(item.precoUnitario(), item.quantidade()));
                    }
                    assertThat(pedido.taxas()).isEqualTo(esperadas);
                });
    }
}
