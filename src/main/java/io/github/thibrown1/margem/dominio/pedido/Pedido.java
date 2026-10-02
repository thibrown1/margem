package io.github.thibrown1.margem.dominio.pedido;

import io.github.thibrown1.margem.dominio.Moeda;
import io.github.thibrown1.margem.dominio.RegraDeNegocioException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Pedido feito na Shopee, vindo da planilha exportada ou da API. */
public class Pedido {

    private final Long id;
    private final String codigo;
    private final OrigemPedido origem;
    private final Instant criadoEm;
    private final List<ItemPedido> itens;
    private StatusPedido status;
    private Instant prazoEnvio;
    private BigDecimal descontoVendedor;
    private TaxasShopee taxas;
    private boolean taxasEstimadas;

    public Pedido(Long id, String codigo, StatusPedido status, OrigemPedido origem, Instant criadoEm, Instant prazoEnvio,
                  BigDecimal descontoVendedor, TaxasShopee taxas, boolean taxasEstimadas, List<ItemPedido> itens) {
        if (codigo == null || codigo.isBlank()) {
            throw new RegraDeNegocioException("O código do pedido é obrigatório");
        }
        if (itens == null || itens.isEmpty()) {
            throw new RegraDeNegocioException("O pedido " + codigo + " não tem itens");
        }
        this.id = id;
        this.codigo = codigo.trim();
        this.status = Objects.requireNonNull(status, "status");
        this.origem = Objects.requireNonNull(origem, "origem");
        this.criadoEm = Objects.requireNonNull(criadoEm, "criadoEm");
        this.prazoEnvio = prazoEnvio;
        this.descontoVendedor = validarDesconto(descontoVendedor);
        this.taxas = taxas == null ? TaxasShopee.NENHUMA : taxas;
        this.taxasEstimadas = taxasEstimadas;
        this.itens = List.copyOf(itens);
    }

    public static Pedido novo(String codigo, StatusPedido status, OrigemPedido origem, Instant criadoEm, Instant prazoEnvio,
                              BigDecimal descontoVendedor, TaxasShopee taxas, boolean taxasEstimadas, List<ItemPedido> itens) {
        return new Pedido(null, codigo, status, origem, criadoEm, prazoEnvio, descontoVendedor, taxas, taxasEstimadas, itens);
    }

    /** Soma dos itens pelo preço de venda (já com os descontos de anúncio), antes de cupom e taxas. */
    public BigDecimal valorBruto() {
        return Moeda.somar(itens.stream().map(ItemPedido::subtotal).toList());
    }

    public int quantidadeDeItens() {
        return itens.stream().mapToInt(ItemPedido::getQuantidade).sum();
    }

    public boolean contaComoVenda() {
        return status.contaComoVenda();
    }

    public boolean temCustoPendente() {
        return itens.stream().anyMatch(ItemPedido::custoPendente);
    }

    /**
     * Atualiza o que muda num pedido ao longo do tempo (status, prazo, taxas).
     * Taxas estimadas nunca sobrescrevem taxas reais já registradas.
     *
     * @return true se algo mudou
     */
    public boolean atualizar(StatusPedido novoStatus, Instant novoPrazo, BigDecimal novoDesconto,
                             TaxasShopee novasTaxas, boolean novasTaxasEstimadas) {
        boolean manterTaxasAtuais = novasTaxasEstimadas && !this.taxasEstimadas;
        TaxasShopee taxasFinais = manterTaxasAtuais || novasTaxas == null ? this.taxas : novasTaxas;
        boolean estimadasFinais = !manterTaxasAtuais && novasTaxas != null ? novasTaxasEstimadas : this.taxasEstimadas;
        BigDecimal descontoFinal = validarDesconto(novoDesconto);

        boolean mudou = status != novoStatus
                || !Objects.equals(prazoEnvio, novoPrazo)
                || descontoVendedor.compareTo(descontoFinal) != 0
                || !taxasFinais.equals(taxas)
                || estimadasFinais != taxasEstimadas;

        status = Objects.requireNonNull(novoStatus, "status");
        prazoEnvio = novoPrazo;
        descontoVendedor = descontoFinal;
        taxas = taxasFinais;
        taxasEstimadas = estimadasFinais;
        return mudou;
    }

    private static BigDecimal validarDesconto(BigDecimal desconto) {
        return desconto == null ? Moeda.ZERO : Moeda.arredondar(desconto.abs());
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public OrigemPedido getOrigem() {
        return origem;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getPrazoEnvio() {
        return prazoEnvio;
    }

    public BigDecimal getDescontoVendedor() {
        return descontoVendedor;
    }

    public TaxasShopee getTaxas() {
        return taxas;
    }

    public boolean isTaxasEstimadas() {
        return taxasEstimadas;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }
}
