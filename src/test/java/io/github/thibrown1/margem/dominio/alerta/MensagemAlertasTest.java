package io.github.thibrown1.margem.dominio.alerta;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MensagemAlertasTest {

    @Test
    void agrupaPorTipoNaOrdemDeUrgencia() {
        List<Alerta> alertas = List.of(
                new Alerta(TipoAlerta.ESTOQUE_BAIXO, "LUMINARIA-LED", "LUMINARIA-LED: 2 un."),
                new Alerta(TipoAlerta.PRAZO_ENVIO, "P1", "Pedido P1"),
                new Alerta(TipoAlerta.PRAZO_ENVIO, "P2", "Pedido P2"));

        String mensagem = MensagemAlertas.montar(alertas, Instant.parse("2026-09-29T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));

        assertThat(mensagem).startsWith("🔔 Margem: alertas de 29/09 às 12:00");
        assertThat(mensagem.indexOf("Envio perto do prazo (2)")).isLessThan(mensagem.indexOf("Estoque baixo (1)"));
        assertThat(mensagem).contains("• Pedido P1\n• Pedido P2");
    }
}
