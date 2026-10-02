package io.github.thibrown1.margem.adaptador.saida.persistencia;

import io.github.thibrown1.margem.adaptador.saida.persistencia.entidade.ItemPedidoEntity;
import io.github.thibrown1.margem.adaptador.saida.persistencia.entidade.PedidoEntity;
import io.github.thibrown1.margem.adaptador.saida.persistencia.springdata.SpringDataPedidoRepository;
import io.github.thibrown1.margem.aplicacao.Pagina;
import io.github.thibrown1.margem.aplicacao.porta.PedidoRepositorio;
import io.github.thibrown1.margem.dominio.pedido.ItemPedido;
import io.github.thibrown1.margem.dominio.pedido.OrigemPedido;
import io.github.thibrown1.margem.dominio.pedido.Pedido;
import io.github.thibrown1.margem.dominio.pedido.StatusPedido;
import io.github.thibrown1.margem.dominio.pedido.TaxasShopee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class PedidoRepositorioAdapter implements PedidoRepositorio {

    private final SpringDataPedidoRepository repositorio;
    private final Clock relogio;

    public PedidoRepositorioAdapter(SpringDataPedidoRepository repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    @Override
    public Optional<Pedido> buscarPorCodigo(String codigo) {
        return repositorio.findByCodigo(codigo.trim()).map(PedidoRepositorioAdapter::paraDominio);
    }

    @Override
    public List<Pedido> buscarPorCodigos(Collection<String> codigos) {
        if (codigos.isEmpty()) {
            return List.of();
        }
        return repositorio.findByCodigoIn(codigos).stream().map(PedidoRepositorioAdapter::paraDominio).toList();
    }

    @Override
    @Transactional
    public Pedido salvar(Pedido pedido) {
        Instant agora = relogio.instant();
        PedidoEntity entidade = pedido.getId() == null
                ? novaEntidade(pedido, agora)
                : repositorio.findById(pedido.getId()).orElseThrow();
        copiarDadosAlteraveis(pedido, entidade, agora);
        return paraDominio(repositorio.save(entidade));
    }

    @Override
    public List<Pedido> buscarCriadosEntre(Instant inicio, Instant fim) {
        return repositorio.buscarComItensCriadosEntre(inicio, fim).stream().map(PedidoRepositorioAdapter::paraDominio).toList();
    }

    @Override
    public Pagina<Pedido> listar(StatusPedido status, Instant inicio, Instant fim, int pagina, int tamanho) {
        PageRequest paginacao = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm"));
        Page<PedidoEntity> resultado = repositorio.filtrar(status == null ? null : status.name(), inicio, fim, paginacao);
        return new Pagina<>(resultado.getContent().stream().map(PedidoRepositorioAdapter::paraDominio).toList(),
                resultado.getNumber(), resultado.getSize(), resultado.getTotalElements(), resultado.getTotalPages());
    }

    @Override
    public List<Pedido> buscarAguardandoEnvio() {
        return repositorio.buscarPorStatusOrdenadoPorPrazo(StatusPedido.AGUARDANDO_ENVIO.name()).stream()
                .map(PedidoRepositorioAdapter::paraDominio)
                .toList();
    }

    @Override
    public long contar() {
        return repositorio.count();
    }

    @Override
    @Transactional
    public int preencherCustoPendente(String sku, BigDecimal custoUnitario) {
        return repositorio.preencherCustoPendente(sku, custoUnitario);
    }

    private static PedidoEntity novaEntidade(Pedido pedido, Instant agora) {
        PedidoEntity entidade = new PedidoEntity(pedido.getCodigo(), pedido.getOrigem().name(), pedido.getCriadoEm(), agora);
        pedido.getItens().forEach(item -> entidade.adicionarItem(new ItemPedidoEntity(item.getSku(), item.getDescricao(),
                item.getQuantidade(), item.getPrecoUnitario(), item.getCustoUnitario())));
        return entidade;
    }

    /** Itens não mudam depois de importados; só status, prazo, cupom e taxas. */
    private static void copiarDadosAlteraveis(Pedido pedido, PedidoEntity entidade, Instant agora) {
        entidade.setStatus(pedido.getStatus().name());
        entidade.setPrazoEnvio(pedido.getPrazoEnvio());
        entidade.setDescontoVendedor(pedido.getDescontoVendedor());
        entidade.setTaxaComissao(pedido.getTaxas().comissao());
        entidade.setTaxaServico(pedido.getTaxas().servico());
        entidade.setTaxaTransacao(pedido.getTaxas().transacao());
        entidade.setTaxasEstimadas(pedido.isTaxasEstimadas());
        entidade.setAtualizadoEm(agora);
    }

    private static Pedido paraDominio(PedidoEntity entidade) {
        List<ItemPedido> itens = entidade.getItens().stream()
                .map(item -> new ItemPedido(item.getId(), item.getSku(), item.getDescricao(), item.getQuantidade(),
                        item.getPrecoUnitario(), item.getCustoUnitario()))
                .toList();
        TaxasShopee taxas = new TaxasShopee(entidade.getTaxaComissao(), entidade.getTaxaServico(), entidade.getTaxaTransacao());
        return new Pedido(entidade.getId(), entidade.getCodigo(), StatusPedido.valueOf(entidade.getStatus()),
                OrigemPedido.valueOf(entidade.getOrigem()), entidade.getCriadoEm(), entidade.getPrazoEnvio(),
                entidade.getDescontoVendedor(), taxas, entidade.isTaxasEstimadas(), itens);
    }
}
