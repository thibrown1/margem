package io.github.thibrown1.margem.aplicacao.pedido;

import io.github.thibrown1.margem.dominio.pedido.StatusPedido;
import io.github.thibrown1.margem.dominio.pedido.TaxasShopee;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Pedido como chega de fora (planilha ou API), antes de virar um {@link io.github.thibrown1.margem.dominio.pedido.Pedido}.
 * Dados pessoais do comprador (nome, endereço, telefone) não fazem parte deste objeto de propósito.
 *
 * @param taxas nulo quando a origem não informa as taxas; nesse caso elas são estimadas pela tabela configurada
 */
public record PedidoImportado(
        String codigo,
        StatusPedido status,
        Instant criadoEm,
        Instant prazoEnvio,
        BigDecimal descontoVendedor,
        TaxasShopee taxas,
        List<ItemImportado> itens) {
}
