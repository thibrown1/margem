package io.github.thibrown1.margem.aplicacao.produto;

import io.github.thibrown1.margem.aplicacao.ConflitoException;
import io.github.thibrown1.margem.aplicacao.RecursoNaoEncontradoException;
import io.github.thibrown1.margem.aplicacao.porta.PedidoRepositorio;
import io.github.thibrown1.margem.aplicacao.porta.ProdutoRepositorio;
import io.github.thibrown1.margem.dominio.produto.Produto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class ProdutoService {

    private final ProdutoRepositorio produtos;
    private final PedidoRepositorio pedidos;

    public ProdutoService(ProdutoRepositorio produtos, PedidoRepositorio pedidos) {
        this.produtos = produtos;
        this.pedidos = pedidos;
    }

    /**
     * Cadastra o produto e, de quebra, preenche o custo dos itens desse SKU que foram vendidos antes do cadastro.
     * Assim o lucro de pedidos antigos deixa de ficar "pendente".
     */
    @Transactional
    public ProdutoCadastrado cadastrar(NovoProduto dados) {
        String sku = Produto.normalizarSku(dados.sku());
        if (produtos.existePorSku(sku)) {
            throw new ConflitoException("Já existe um produto com o SKU " + sku);
        }
        Produto salvo = produtos.salvar(Produto.novo(sku, dados.nome(), dados.custoUnitario(),
                dados.estoqueInicial(), dados.estoqueMinimo()));
        int itensAtualizados = pedidos.preencherCustoPendente(sku, salvo.getCustoUnitario());
        return new ProdutoCadastrado(salvo, itensAtualizados);
    }

    /** Mudar o custo aqui vale só para vendas futuras: o custo de pedidos já importados fica congelado. */
    @Transactional
    public Produto atualizar(String sku, String nome, BigDecimal custoUnitario, int estoqueMinimo) {
        Produto produto = buscar(sku);
        produto.atualizar(nome, custoUnitario, estoqueMinimo);
        return produtos.salvar(produto);
    }

    @Transactional
    public Produto registrarEntrada(String sku, int quantidade, BigDecimal custoUnitario) {
        Produto produto = buscar(sku);
        produto.registrarEntrada(quantidade, custoUnitario);
        return produtos.salvar(produto);
    }

    @Transactional(readOnly = true)
    public Produto buscar(String sku) {
        String normalizado = Produto.normalizarSku(sku);
        return produtos.buscarPorSku(normalizado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto " + normalizado + " não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<Produto> listar() {
        return produtos.listarTodos().stream()
                .sorted(Comparator.comparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Estoque do mais urgente (sem estoque) para o mais tranquilo. */
    @Transactional(readOnly = true)
    public List<ItemEstoque> estoque() {
        return produtos.listarTodos().stream()
                .sorted(Comparator.comparing(Produto::situacaoEstoque)
                        .thenComparing(Produto::getEstoqueAtual)
                        .thenComparing(Produto::getSku))
                .map(p -> new ItemEstoque(p.getSku(), p.getNome(), p.getEstoqueAtual(), p.getEstoqueMinimo(),
                        p.situacaoEstoque()))
                .toList();
    }
}
