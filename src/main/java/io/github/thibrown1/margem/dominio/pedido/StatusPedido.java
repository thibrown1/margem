package io.github.thibrown1.margem.dominio.pedido;

/**
 * Status do pedido, independente de como a Shopee escreve (texto na planilha ou código na API).
 * Só entra no faturamento e baixa estoque o pedido que virou venda de fato.
 */
public enum StatusPedido {
    NAO_PAGO(false),
    AGUARDANDO_ENVIO(true),
    ENVIADO(true),
    CONCLUIDO(true),
    CANCELADO(false),
    DEVOLVIDO(false);

    private final boolean venda;

    StatusPedido(boolean venda) {
        this.venda = venda;
    }

    public boolean contaComoVenda() {
        return venda;
    }

    public boolean foiDesfeito() {
        return this == CANCELADO || this == DEVOLVIDO;
    }
}
