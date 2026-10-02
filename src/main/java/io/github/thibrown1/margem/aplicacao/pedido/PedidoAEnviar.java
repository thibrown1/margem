package io.github.thibrown1.margem.aplicacao.pedido;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Pedido na fila de expedição.
 *
 * @param horasRestantes horas até o prazo de envio; negativo quando já passou do prazo
 * @param itens          o que separar, no formato "2x Garrafa Térmica (GARRAFA-INOX-500)"
 */
public record PedidoAEnviar(
        String codigo,
        OffsetDateTime criadoEm,
        OffsetDateTime prazoEnvio,
        Long horasRestantes,
        boolean atrasado,
        List<String> itens) {
}
