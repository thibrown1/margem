package io.github.thibrown1.margem.aplicacao.alerta;

import io.github.thibrown1.margem.dominio.alerta.Alerta;

import java.util.List;

/**
 * @param alertasEnviados   alertas novos que foram enviados agora
 * @param alertasRepetidos  alertas que continuam valendo mas já tinham sido enviados recentemente
 */
public record ResultadoAlertas(String canal, int alertasRepetidos, List<Alerta> alertasEnviados) {
}
