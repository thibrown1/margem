package io.github.thibrown1.margem.suporte;

import io.github.thibrown1.margem.aplicacao.Pagina;
import io.github.thibrown1.margem.aplicacao.porta.PedidoRepositorio;
import io.github.thibrown1.margem.dominio.pedido.ItemPedido;
import io.github.thibrown1.margem.dominio.pedido.Pedido;
import io.github.thibrown1.margem.dominio.pedido.StatusPedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class PedidoRepositorioEmMemoria implements PedidoRepositorio {

    private final Map<String, Pedido> pedidos = new LinkedHashMap<>();
    private final AtomicLong sequencia = new AtomicLong();

    @Override
    public Optional<Pedido> buscarPorCodigo(String codigo) {
        return Optional.ofNullable(pedidos.get(codigo));
    }

    @Override
    public List<Pedido> buscarPorCodigos(Collection<String> codigos) {
        return codigos.stream().map(pedidos::get).filter(p -> p != null).toList();
    }

    @Override
    public Pedido salvar(Pedido pedido) {
        Pedido comId = pedido.getId() != null ? pedido : new Pedido(sequencia.incrementAndGet(), pedido.getCodigo(),
                pedido.getStatus(), pedido.getOrigem(), pedido.getCriadoEm(), pedido.getPrazoEnvio(),
                pedido.getDescontoVendedor(), pedido.getTaxas(), pedido.isTaxasEstimadas(), pedido.getItens());
        pedidos.put(comId.getCodigo(), comId);
        return comId;
    }

    @Override
    public List<Pedido> buscarCriadosEntre(Instant inicio, Instant fim) {
        return pedidos.values().stream()
                .filter(p -> !p.getCriadoEm().isBefore(inicio) && p.getCriadoEm().isBefore(fim))
                .sorted(Comparator.comparing(Pedido::getCriadoEm))
                .toList();
    }

    @Override
    public Pagina<Pedido> listar(StatusPedido status, Instant inicio, Instant fim, int pagina, int tamanho) {
        List<Pedido> filtrados = buscarCriadosEntre(inicio, fim).stream()
                .filter(p -> status == null || p.getStatus() == status)
                .toList();
        List<Pedido> itens = filtrados.stream().skip((long) pagina * tamanho).limit(tamanho).toList();
        return new Pagina<>(itens, pagina, tamanho, filtrados.size(), (filtrados.size() + tamanho - 1) / tamanho);
    }

    @Override
    public List<Pedido> buscarAguardandoEnvio() {
        return pedidos.values().stream()
                .filter(p -> p.getStatus() == StatusPedido.AGUARDANDO_ENVIO)
                .sorted(Comparator.comparing(Pedido::getPrazoEnvio, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Override
    public long contar() {
        return pedidos.size();
    }

    @Override
    public int preencherCustoPendente(String sku, BigDecimal custoUnitario) {
        int atualizados = 0;
        for (Pedido pedido : List.copyOf(pedidos.values())) {
            boolean temPendente = pedido.getItens().stream().anyMatch(i -> i.getSku().equals(sku) && i.custoPendente());
            if (!temPendente) {
                continue;
            }
            List<ItemPedido> itens = pedido.getItens().stream()
                    .map(i -> i.getSku().equals(sku) && i.custoPendente()
                            ? new ItemPedido(i.getId(), i.getSku(), i.getDescricao(), i.getQuantidade(), i.getPrecoUnitario(), custoUnitario)
                            : i)
                    .toList();
            atualizados += (int) pedido.getItens().stream().filter(i -> i.getSku().equals(sku) && i.custoPendente()).count();
            pedidos.put(pedido.getCodigo(), new Pedido(pedido.getId(), pedido.getCodigo(), pedido.getStatus(), pedido.getOrigem(),
                    pedido.getCriadoEm(), pedido.getPrazoEnvio(), pedido.getDescontoVendedor(), pedido.getTaxas(),
                    pedido.isTaxasEstimadas(), itens));
        }
        return atualizados;
    }
}
