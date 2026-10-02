package io.github.thibrown1.margem.adaptador.saida.persistencia;

import io.github.thibrown1.margem.adaptador.saida.persistencia.entidade.ProdutoEntity;
import io.github.thibrown1.margem.adaptador.saida.persistencia.springdata.SpringDataProdutoRepository;
import io.github.thibrown1.margem.aplicacao.porta.ProdutoRepositorio;
import io.github.thibrown1.margem.dominio.produto.Produto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Implementa a porta de produtos com JPA, convertendo entre entidade e modelo de domínio. */
@Component
@Transactional(readOnly = true)
public class ProdutoRepositorioAdapter implements ProdutoRepositorio {

    private final SpringDataProdutoRepository repositorio;
    private final Clock relogio;

    public ProdutoRepositorioAdapter(SpringDataProdutoRepository repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    @Override
    public Optional<Produto> buscarPorSku(String sku) {
        return repositorio.findBySku(sku).map(ProdutoRepositorioAdapter::paraDominio);
    }

    @Override
    public List<Produto> buscarPorSkus(Collection<String> skus) {
        if (skus.isEmpty()) {
            return List.of();
        }
        return repositorio.findBySkuIn(skus).stream().map(ProdutoRepositorioAdapter::paraDominio).toList();
    }

    @Override
    public List<Produto> listarTodos() {
        return repositorio.findAll().stream().map(ProdutoRepositorioAdapter::paraDominio).toList();
    }

    @Override
    public boolean existePorSku(String sku) {
        return repositorio.existsBySku(sku);
    }

    @Override
    @Transactional
    public Produto salvar(Produto produto) {
        return paraDominio(repositorio.save(paraEntidade(produto, relogio.instant())));
    }

    @Override
    @Transactional
    public void salvarTodos(Collection<Produto> produtos) {
        Instant agora = relogio.instant();
        repositorio.saveAll(produtos.stream().map(p -> paraEntidade(p, agora)).toList());
    }

    private ProdutoEntity paraEntidade(Produto produto, Instant agora) {
        ProdutoEntity entidade = produto.getId() == null
                ? new ProdutoEntity(produto.getSku(), agora)
                : repositorio.findById(produto.getId()).orElseThrow();
        entidade.setNome(produto.getNome());
        entidade.setCustoUnitario(produto.getCustoUnitario());
        entidade.setEstoqueAtual(produto.getEstoqueAtual());
        entidade.setEstoqueMinimo(produto.getEstoqueMinimo());
        entidade.setAtualizadoEm(agora);
        return entidade;
    }

    private static Produto paraDominio(ProdutoEntity entidade) {
        return new Produto(entidade.getId(), entidade.getSku(), entidade.getNome(), entidade.getCustoUnitario(),
                entidade.getEstoqueAtual(), entidade.getEstoqueMinimo());
    }
}
