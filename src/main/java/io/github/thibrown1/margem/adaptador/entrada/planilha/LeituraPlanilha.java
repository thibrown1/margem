package io.github.thibrown1.margem.adaptador.entrada.planilha;

import io.github.thibrown1.margem.aplicacao.pedido.ErroImportacao;
import io.github.thibrown1.margem.aplicacao.pedido.PedidoImportado;

import java.util.List;

/**
 * @param pedidos pedidos lidos sem erro
 * @param erros   linhas com problema; o pedido inteiro dessas linhas fica de fora para não gerar lucro errado
 */
public record LeituraPlanilha(List<PedidoImportado> pedidos, List<ErroImportacao> erros) {
}
