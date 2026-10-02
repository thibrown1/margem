package io.github.thibrown1.margem.aplicacao.porta;

import io.github.thibrown1.margem.dominio.alerta.Alerta;
import io.github.thibrown1.margem.dominio.alerta.TipoAlerta;

import java.time.Instant;
import java.util.List;

/** Guarda quais alertas já foram enviados, para o mesmo aviso não chegar de hora em hora. */
public interface AlertaRepositorio {

    boolean foiEnviadoDesde(TipoAlerta tipo, String referencia, Instant desde);

    void registrarEnvio(List<Alerta> alertas, Instant enviadoEm);
}
