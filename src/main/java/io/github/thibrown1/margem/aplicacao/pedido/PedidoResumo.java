package io.github.thibrown1.margem.aplicacao.pedido;

import io.github.thibrown1.margem.dominio.pedido.OrigemPedido;
import io.github.thibrown1.margem.dominio.pedido.StatusPedido;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PedidoResumo(
        String codigo,
        StatusPedido status,
        OrigemPedido origem,
        OffsetDateTime criadoEm,
        OffsetDateTime prazoEnvio,
        int quantidadeDeItens,
        BigDecimal valorBruto) {
}
