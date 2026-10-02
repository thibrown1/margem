package io.github.thibrown1.margem.adaptador.entrada.agendamento;

import io.github.thibrown1.margem.aplicacao.alerta.AlertaService;
import io.github.thibrown1.margem.aplicacao.alerta.ResultadoAlertas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Verifica os alertas nos horários de margem.alertas.cron (padrão: de hora em hora, das 8h às 20h). */
@Component
@ConditionalOnProperty(prefix = "margem.alertas", name = "habilitado", havingValue = "true")
public class AlertasJob {

    private static final Logger log = LoggerFactory.getLogger(AlertasJob.class);

    private final AlertaService alertas;

    public AlertasJob(AlertaService alertas) {
        this.alertas = alertas;
    }

    @Scheduled(cron = "${margem.alertas.cron}", zone = "${margem.fuso-horario}")
    public void executar() {
        try {
            ResultadoAlertas resultado = alertas.verificarEEnviar();
            log.info("Alertas verificados: {} enviados por {}, {} já avisados antes",
                    resultado.alertasEnviados().size(), resultado.canal(), resultado.alertasRepetidos());
        } catch (RuntimeException e) {
            log.error("Falha ao verificar alertas; nova tentativa no próximo horário", e);
        }
    }
}
