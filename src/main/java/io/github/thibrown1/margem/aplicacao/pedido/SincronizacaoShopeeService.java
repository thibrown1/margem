package io.github.thibrown1.margem.aplicacao.pedido;

import io.github.thibrown1.margem.aplicacao.porta.ShopeeGateway;
import io.github.thibrown1.margem.dominio.RegraDeNegocioException;
import io.github.thibrown1.margem.dominio.pedido.OrigemPedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Busca os pedidos dos últimos dias na Shopee e importa. */
@Service
public class SincronizacaoShopeeService {

    public static final int MAXIMO_DE_DIAS = 90;

    private static final Logger log = LoggerFactory.getLogger(SincronizacaoShopeeService.class);

    private final ShopeeGateway shopee;
    private final ImportacaoPedidosService importacao;
    private final Clock relogio;

    public SincronizacaoShopeeService(ShopeeGateway shopee, ImportacaoPedidosService importacao, Clock relogio) {
        this.shopee = shopee;
        this.importacao = importacao;
        this.relogio = relogio;
    }

    public ResultadoImportacao sincronizar(int dias) {
        if (dias < 1 || dias > MAXIMO_DE_DIAS) {
            throw new RegraDeNegocioException("Sincronize entre 1 e " + MAXIMO_DE_DIAS + " dias");
        }
        Instant ate = relogio.instant();
        Instant de = ate.minus(Duration.ofDays(dias));
        List<PedidoImportado> encontrados = shopee.buscarPedidos(de, ate);
        ResultadoImportacao resultado = importacao.importar(encontrados, OrigemPedido.API_SHOPEE);
        log.info("Sincronização com {} ({} dias): {} novos, {} atualizados, {} sem alteração",
                shopee.descricao(), dias, resultado.pedidosNovos(), resultado.pedidosAtualizados(),
                resultado.pedidosSemAlteracao());
        return resultado;
    }
}
