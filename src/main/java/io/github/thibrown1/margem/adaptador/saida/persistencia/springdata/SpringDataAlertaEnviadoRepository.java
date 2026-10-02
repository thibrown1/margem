package io.github.thibrown1.margem.adaptador.saida.persistencia.springdata;

import io.github.thibrown1.margem.adaptador.saida.persistencia.entidade.AlertaEnviadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface SpringDataAlertaEnviadoRepository extends JpaRepository<AlertaEnviadoEntity, Long> {

    boolean existsByTipoAndReferenciaAndEnviadoEmAfter(String tipo, String referencia, Instant desde);
}
