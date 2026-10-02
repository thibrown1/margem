package io.github.thibrown1.margem.aplicacao.porta;

import io.github.thibrown1.margem.aplicacao.pedido.PedidoImportado;

import java.time.Instant;
import java.util.List;

/**
 * Porta de saída para buscar pedidos na Shopee. Existem duas implementações:
 * a API oficial (Open Platform v2) e uma Shopee simulada, usada na demonstração e em desenvolvimento.
 */
public interface ShopeeGateway {

    List<PedidoImportado> buscarPedidos(Instant de, Instant ate);

    String descricao();
}
