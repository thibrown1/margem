package io.github.thibrown1.margem.dominio.alerta;

/**
 * Algo que o dono da loja precisa saber agora.
 *
 * @param referencia o que o alerta identifica (código do pedido, SKU); usado para não repetir o mesmo aviso
 */
public record Alerta(TipoAlerta tipo, String referencia, String mensagem) {
}
