package io.github.thibrown1.margem.dominio.pedido;

import io.github.thibrown1.margem.dominio.Moeda;
import io.github.thibrown1.margem.dominio.RegraDeNegocioException;
import io.github.thibrown1.margem.dominio.produto.Produto;

import java.math.BigDecimal;

/**
 * Item vendido. O custo unitário é uma "foto" do custo do produto no momento da venda:
 * se o custo mudar depois (compra nova mais cara), o lucro dos pedidos antigos não muda.
 * Custo nulo significa que o SKU ainda não estava cadastrado quando o pedido chegou.
 */
public class ItemPedido {

    private final Long id;
    private final String sku;
    private final String descricao;
    private final int quantidade;
    private final BigDecimal precoUnitario;
    private BigDecimal custoUnitario;

    public ItemPedido(Long id, String sku, String descricao, int quantidade, BigDecimal precoUnitario, BigDecimal custoUnitario) {
        if (quantidade <= 0) {
            throw new RegraDeNegocioException("A quantidade do item deve ser maior que zero");
        }
        if (precoUnitario == null || precoUnitario.signum() < 0) {
            throw new RegraDeNegocioException("O preço do item não pode ser negativo");
        }
        this.id = id;
        this.sku = Produto.normalizarSku(sku);
        this.descricao = descricao == null || descricao.isBlank() ? this.sku : descricao.trim();
        this.quantidade = quantidade;
        this.precoUnitario = Moeda.arredondar(precoUnitario);
        this.custoUnitario = Moeda.arredondar(custoUnitario);
    }

    public static ItemPedido novo(String sku, String descricao, int quantidade, BigDecimal precoUnitario, BigDecimal custoUnitario) {
        return new ItemPedido(null, sku, descricao, quantidade, precoUnitario, custoUnitario);
    }

    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public boolean custoPendente() {
        return custoUnitario == null;
    }

    /** Custo total do item; zero quando o custo ainda é desconhecido (veja {@link #custoPendente()}). */
    public BigDecimal custoTotal() {
        return custoPendente() ? Moeda.ZERO : custoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public BigDecimal getCustoUnitario() {
        return custoUnitario;
    }
}
