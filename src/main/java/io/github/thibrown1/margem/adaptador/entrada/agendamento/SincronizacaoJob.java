package io.github.thibrown1.margem.adaptador.entrada.agendamento;

import io.github.thibrown1.margem.aplicacao.pedido.SincronizacaoShopeeService;
import io.github.thibrown1.margem.configuracao.MargemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Busca pedidos novos na Shopee periodicamente (margem.shopee.sincronizacao.cron). */
@Component
@ConditionalOnProperty(prefix = "margem.shopee.sincronizacao", name = "habilitada", havingValue = "true")
public class SincronizacaoJob {

    private static final Logger log = LoggerFactory.getLogger(SincronizacaoJob.class);

    private final SincronizacaoShopeeService sincronizacao;
    private final int dias;

    public SincronizacaoJob(SincronizacaoShopeeService sincronizacao, MargemProperties propriedades) {
        this.sincronizacao = sincronizacao;
        this.dias = propriedades.shopee().sincronizacao().dias();
    }

    @Scheduled(cron = "${margem.shopee.sincronizacao.cron}", zone = "${margem.fuso-horario}")
    public void executar() {
        try {
            sincronizacao.sincronizar(dias);
        } catch (RuntimeException e) {
            log.error("Falha ao sincronizar com a Shopee; nova tentativa no próximo horário", e);
        }
    }
}
