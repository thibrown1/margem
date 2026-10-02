package io.github.thibrown1.margem.aplicacao;

import java.util.List;
import java.util.function.Function;

/** Página de resultados, sem depender do Page do Spring Data na camada de aplicação. */
public record Pagina<T>(List<T> itens, int pagina, int tamanho, long totalDeItens, int totalDePaginas) {

    public <R> Pagina<R> mapear(Function<T, R> conversor) {
        return new Pagina<>(itens.stream().map(conversor).toList(), pagina, tamanho, totalDeItens, totalDePaginas);
    }
}
