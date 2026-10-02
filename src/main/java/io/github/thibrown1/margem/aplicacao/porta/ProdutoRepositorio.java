package io.github.thibrown1.margem.aplicacao.porta;

import io.github.thibrown1.margem.dominio.produto.Produto;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProdutoRepositorio {

    Optional<Produto> buscarPorSku(String sku);

    List<Produto> buscarPorSkus(Collection<String> skus);

    List<Produto> listarTodos();

    boolean existePorSku(String sku);

    Produto salvar(Produto produto);

    void salvarTodos(Collection<Produto> produtos);
}
