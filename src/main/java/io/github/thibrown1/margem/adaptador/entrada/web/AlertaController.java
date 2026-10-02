package io.github.thibrown1.margem.adaptador.entrada.web;

import io.github.thibrown1.margem.aplicacao.alerta.AlertaService;
import io.github.thibrown1.margem.aplicacao.alerta.ResultadoAlertas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alertas")
@Tag(name = "7. Alertas", description = "Prazo de envio, estoque baixo e pedidos no prejuízo, enviados pelo Telegram.")
public class AlertaController {

    private final AlertaService alertas;

    public AlertaController(AlertaService alertas) {
        this.alertas = alertas;
    }

    @PostMapping("/verificacoes")
    @Operation(summary = "Verifica agora e envia os alertas novos",
            description = "A mesma verificação roda sozinha nos horários configurados (margem.alertas.cron).")
    public ResultadoAlertas verificar() {
        return alertas.verificarEEnviar();
    }
}
