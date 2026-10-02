package io.github.thibrown1.margem.adaptador.saida.persistencia;

import io.github.thibrown1.margem.adaptador.saida.persistencia.entidade.AlertaEnviadoEntity;
import io.github.thibrown1.margem.adaptador.saida.persistencia.springdata.SpringDataAlertaEnviadoRepository;
import io.github.thibrown1.margem.aplicacao.porta.AlertaRepositorio;
import io.github.thibrown1.margem.dominio.alerta.Alerta;
import io.github.thibrown1.margem.dominio.alerta.TipoAlerta;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class AlertaRepositorioAdapter implements AlertaRepositorio {

    private final SpringDataAlertaEnviadoRepository repositorio;

    public AlertaRepositorioAdapter(SpringDataAlertaEnviadoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean foiEnviadoDesde(TipoAlerta tipo, String referencia, Instant desde) {
        return repositorio.existsByTipoAndReferenciaAndEnviadoEmAfter(tipo.name(), referencia, desde);
    }

    @Override
    @Transactional
    public void registrarEnvio(List<Alerta> alertas, Instant enviadoEm) {
        repositorio.saveAll(alertas.stream()
                .map(alerta -> new AlertaEnviadoEntity(alerta.tipo().name(), alerta.referencia(), enviadoEm))
                .toList());
    }
}
