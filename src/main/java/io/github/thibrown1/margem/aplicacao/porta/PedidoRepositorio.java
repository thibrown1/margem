package io.github.thibrown1.margem.aplicacao.porta;

import io.github.thibrown1.margem.aplicacao.Pagina;
import io.github.thibrown1.margem.dominio.pedido.Pedido;
import io.github.thibrown1.margem.dominio.pedido.StatusPedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PedidoRepositorio {

    Optional<Pedido> buscarPorCodigo(String codigo);

    List<Pedido> buscarPorCodigos(Collection<String> codigos);

    Pedido salvar(Pedido pedido);

    /** Pedidos criados no intervalo [inicio, fim), já com os itens. */
    List<Pedido> buscarCriadosEntre(Instant inicio, Instant fim);

    Pagina<Pedido> listar(StatusPedido status, Instant inicio, Instant fim, int pagina, int tamanho);

    /** Pedidos pagos que ainda não foram enviados, do prazo mais apertado para o mais folgado. */
    List<Pedido> buscarAguardandoEnvio();

    long contar();

    /**
     * Preenche o custo dos itens vendidos antes do produto ser cadastrado.
     *
     * @return quantos itens foram atualizados
     */
    int preencherCustoPendente(String sku, BigDecimal custoUnitario);
}
