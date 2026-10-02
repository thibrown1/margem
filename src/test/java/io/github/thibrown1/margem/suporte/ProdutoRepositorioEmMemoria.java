package io.github.thibrown1.margem.suporte;

import io.github.thibrown1.margem.aplicacao.porta.ProdutoRepositorio;
import io.github.thibrown1.margem.dominio.produto.Produto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** Implementação da porta em memória: testa os casos de uso sem banco e sem Spring. */
public class ProdutoRepositorioEmMemoria implements ProdutoRepositorio {

    private final Map<String, Produto> produtos = new LinkedHashMap<>();
    private final AtomicLong sequencia = new AtomicLong();

    public Produto cadastrar(Produto produto) {
        return salvar(produto);
    }

    @Override
    public Optional<Produto> buscarPorSku(String sku) {
        return Optional.ofNullable(produtos.get(sku));
    }

    @Override
    public List<Produto> buscarPorSkus(Collection<String> skus) {
        return skus.stream().map(produtos::get).filter(p -> p != null).toList();
    }

    @Override
    public List<Produto> listarTodos() {
        return new ArrayList<>(produtos.values());
    }

    @Override
    public boolean existePorSku(String sku) {
        return produtos.containsKey(sku);
    }

    @Override
    public Produto salvar(Produto produto) {
        Produto comId = produto.getId() != null ? produto : new Produto(sequencia.incrementAndGet(), produto.getSku(),
                produto.getNome(), produto.getCustoUnitario(), produto.getEstoqueAtual(), produto.getEstoqueMinimo());
        produtos.put(comId.getSku(), comId);
        return comId;
    }

    @Override
    public void salvarTodos(Collection<Produto> lista) {
        lista.forEach(this::salvar);
    }
}
