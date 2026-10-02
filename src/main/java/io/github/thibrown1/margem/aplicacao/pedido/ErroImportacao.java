package io.github.thibrown1.margem.aplicacao.pedido;

/**
 * Problema encontrado ao importar.
 *
 * @param linha  linha da planilha (como aparece no Excel); nulo quando o erro não vem de planilha
 * @param pedido código do pedido afetado, se conhecido
 */
public record ErroImportacao(Integer linha, String pedido, String motivo) {
}
