package io.github.thibrown1.margem.dominio.produto;

import io.github.thibrown1.margem.dominio.Moeda;
import io.github.thibrown1.margem.dominio.RegraDeNegocioException;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Produto do catálogo da loja. O SKU é o mesmo código cadastrado no anúncio da Shopee,
 * e é por ele que um item vendido é ligado ao seu custo.
 */
public class Produto {

    private final Long id;
    private final String sku;
    private String nome;
    private BigDecimal custoUnitario;
    private int estoqueAtual;
    private int estoqueMinimo;

    public Produto(Long id, String sku, String nome, BigDecimal custoUnitario, int estoqueAtual, int estoqueMinimo) {
        this.id = id;
        this.sku = normalizarSku(sku);
        definirNome(nome);
        definirCusto(custoUnitario);
        definirEstoqueMinimo(estoqueMinimo);
        this.estoqueAtual = estoqueAtual;
    }

    public static Produto novo(String sku, String nome, BigDecimal custoUnitario, int estoqueInicial, int estoqueMinimo) {
        if (estoqueInicial < 0) {
            throw new RegraDeNegocioException("O estoque inicial não pode ser negativo");
        }
        return new Produto(null, sku, nome, custoUnitario, estoqueInicial, estoqueMinimo);
    }

    public static String normalizarSku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new RegraDeNegocioException("O SKU é obrigatório");
        }
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Registra a chegada de mercadoria e recalcula o custo médio ponderado.
     * Ex.: 10 un. a R$ 20,00 + 10 un. a R$ 30,00 = 20 un. a R$ 25,00.
     * Se o estoque estava zerado (ou negativo), o custo passa a ser o da compra nova.
     */
    public void registrarEntrada(int quantidade, BigDecimal custoUnitarioDaCompra) {
        if (quantidade <= 0) {
            throw new RegraDeNegocioException("A quantidade da entrada deve ser maior que zero");
        }
        if (custoUnitarioDaCompra == null || custoUnitarioDaCompra.signum() < 0) {
            throw new RegraDeNegocioException("O custo da compra não pode ser negativo");
        }
        if (estoqueAtual <= 0) {
            custoUnitario = Moeda.arredondar(custoUnitarioDaCompra);
        } else {
            BigDecimal valorEmEstoque = custoUnitario.multiply(BigDecimal.valueOf(estoqueAtual));
            BigDecimal valorDaCompra = custoUnitarioDaCompra.multiply(BigDecimal.valueOf(quantidade));
            BigDecimal quantidadeTotal = BigDecimal.valueOf((long) estoqueAtual + quantidade);
            custoUnitario = valorEmEstoque.add(valorDaCompra).divide(quantidadeTotal, Moeda.ESCALA, Moeda.ARREDONDAMENTO);
        }
        estoqueAtual += quantidade;
    }

    /**
     * Baixa o estoque por uma venda. Pode ficar negativo de propósito: a venda já aconteceu na Shopee,
     * então o sistema registra a realidade e o alerta de estoque avisa que o cadastro está furado.
     */
    public void baixarEstoque(int quantidade) {
        estoqueAtual -= quantidade;
    }

    /** Devolve ao estoque os itens de um pedido cancelado ou devolvido. */
    public void devolverAoEstoque(int quantidade) {
        estoqueAtual += quantidade;
    }

    public void atualizar(String nome, BigDecimal custoUnitario, int estoqueMinimo) {
        definirNome(nome);
        definirCusto(custoUnitario);
        definirEstoqueMinimo(estoqueMinimo);
    }

    public SituacaoEstoque situacaoEstoque() {
        if (estoqueAtual <= 0) {
            return SituacaoEstoque.SEM_ESTOQUE;
        }
        if (estoqueAtual <= estoqueMinimo) {
            return SituacaoEstoque.BAIXO;
        }
        return SituacaoEstoque.OK;
    }

    private void definirNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("O nome do produto é obrigatório");
        }
        this.nome = nome.trim();
    }

    private void definirCusto(BigDecimal custo) {
        if (custo == null || custo.signum() < 0) {
            throw new RegraDeNegocioException("O custo do produto não pode ser negativo");
        }
        this.custoUnitario = Moeda.arredondar(custo);
    }

    private void definirEstoqueMinimo(int estoqueMinimo) {
        if (estoqueMinimo < 0) {
            throw new RegraDeNegocioException("O estoque mínimo não pode ser negativo");
        }
        this.estoqueMinimo = estoqueMinimo;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getCustoUnitario() {
        return custoUnitario;
    }

    public int getEstoqueAtual() {
        return estoqueAtual;
    }

    public int getEstoqueMinimo() {
        return estoqueMinimo;
    }
}
