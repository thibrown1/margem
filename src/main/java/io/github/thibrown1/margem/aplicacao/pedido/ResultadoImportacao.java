package io.github.thibrown1.margem.aplicacao.pedido;

import java.util.ArrayList;
import java.util.List;

public record ResultadoImportacao(
        int pedidosNovos,
        int pedidosAtualizados,
        int pedidosSemAlteracao,
        List<String> skusSemCadastro,
        List<ErroImportacao> erros,
        List<String> avisos) {

    public ResultadoImportacao comErrosDeLeitura(List<ErroImportacao> errosDeLeitura) {
        if (errosDeLeitura.isEmpty()) {
            return this;
        }
        List<ErroImportacao> todos = new ArrayList<>(errosDeLeitura);
        todos.addAll(erros);
        return new ResultadoImportacao(pedidosNovos, pedidosAtualizados, pedidosSemAlteracao, skusSemCadastro,
                List.copyOf(todos), avisos);
    }
}
