package io.github.thibrown1.margem.dominio.alerta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Monta um único texto com todos os alertas, agrupados por tipo, pronto para mandar no Telegram. */
public final class MensagemAlertas {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    private MensagemAlertas() {
    }

    public static String montar(List<Alerta> alertas, Instant agora, ZoneId zona) {
        Map<TipoAlerta, List<Alerta>> porTipo = alertas.stream()
                .collect(Collectors.groupingBy(Alerta::tipo, TreeMap::new, Collectors.toList()));

        StringBuilder texto = new StringBuilder("🔔 Margem: alertas de ")
                .append(DATA_HORA.format(agora.atZone(zona)));
        porTipo.forEach((tipo, lista) -> {
            texto.append("\n\n").append(tipo.getIcone()).append(' ').append(tipo.getTitulo())
                    .append(" (").append(lista.size()).append(')');
            lista.forEach(alerta -> texto.append("\n• ").append(alerta.mensagem()));
        });
        return texto.toString();
    }
}
