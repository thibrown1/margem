package io.github.thibrown1.margem.configuracao;

import io.github.thibrown1.margem.aplicacao.pedido.ResultadoImportacao;
import io.github.thibrown1.margem.aplicacao.pedido.SincronizacaoShopeeService;
import io.github.thibrown1.margem.aplicacao.porta.PedidoRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * No perfil "demo", preenche o banco vazio com 45 dias de pedidos da Shopee simulada,
 * para quem abrir o projeto já ver relatórios com dados.
 */
@Component
@Profile("demo")
public class DadosDemonstracaoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosDemonstracaoRunner.class);
    private static final int DIAS_DE_HISTORICO = 45;

    private final PedidoRepositorio pedidos;
    private final SincronizacaoShopeeService sincronizacao;
    private final MargemProperties propriedades;

    public DadosDemonstracaoRunner(PedidoRepositorio pedidos, SincronizacaoShopeeService sincronizacao,
                                   MargemProperties propriedades) {
        this.pedidos = pedidos;
        this.sincronizacao = sincronizacao;
        this.propriedades = propriedades;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (propriedades.shopee().modo() != MargemProperties.Shopee.Modo.SIMULADA || pedidos.contar() > 0) {
            return;
        }
        ResultadoImportacao resultado = sincronizacao.sincronizar(DIAS_DE_HISTORICO);
        log.info("Demonstração pronta: {} pedidos simulados dos últimos {} dias", resultado.pedidosNovos(), DIAS_DE_HISTORICO);
    }
}
