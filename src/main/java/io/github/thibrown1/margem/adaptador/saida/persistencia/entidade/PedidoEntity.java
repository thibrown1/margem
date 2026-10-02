package io.github.thibrown1.margem.adaptador.saida.persistencia.entidade;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido")
public class PedidoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40, updatable = false)
    private String codigo;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, length = 20, updatable = false)
    private String origem;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "prazo_envio")
    private Instant prazoEnvio;

    @Column(name = "desconto_vendedor", nullable = false, precision = 12, scale = 2)
    private BigDecimal descontoVendedor;

    @Column(name = "taxa_comissao", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxaComissao;

    @Column(name = "taxa_servico", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxaServico;

    @Column(name = "taxa_transacao", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxaTransacao;

    @Column(name = "taxas_estimadas", nullable = false)
    private boolean taxasEstimadas;

    @Version
    @Column(nullable = false)
    private long versao;

    @Column(name = "importado_em", nullable = false, updatable = false)
    private Instant importadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ItemPedidoEntity> itens = new ArrayList<>();

    protected PedidoEntity() {
    }

    public PedidoEntity(String codigo, String origem, Instant criadoEm, Instant importadoEm) {
        this.codigo = codigo;
        this.origem = origem;
        this.criadoEm = criadoEm;
        this.importadoEm = importadoEm;
    }

    public void adicionarItem(ItemPedidoEntity item) {
        item.setPedido(this);
        itens.add(item);
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrigem() {
        return origem;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getPrazoEnvio() {
        return prazoEnvio;
    }

    public void setPrazoEnvio(Instant prazoEnvio) {
        this.prazoEnvio = prazoEnvio;
    }

    public BigDecimal getDescontoVendedor() {
        return descontoVendedor;
    }

    public void setDescontoVendedor(BigDecimal descontoVendedor) {
        this.descontoVendedor = descontoVendedor;
    }

    public BigDecimal getTaxaComissao() {
        return taxaComissao;
    }

    public void setTaxaComissao(BigDecimal taxaComissao) {
        this.taxaComissao = taxaComissao;
    }

    public BigDecimal getTaxaServico() {
        return taxaServico;
    }

    public void setTaxaServico(BigDecimal taxaServico) {
        this.taxaServico = taxaServico;
    }

    public BigDecimal getTaxaTransacao() {
        return taxaTransacao;
    }

    public void setTaxaTransacao(BigDecimal taxaTransacao) {
        this.taxaTransacao = taxaTransacao;
    }

    public boolean isTaxasEstimadas() {
        return taxasEstimadas;
    }

    public void setTaxasEstimadas(boolean taxasEstimadas) {
        this.taxasEstimadas = taxasEstimadas;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public List<ItemPedidoEntity> getItens() {
        return itens;
    }
}
