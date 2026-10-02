package io.github.thibrown1.margem.adaptador.saida.persistencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "alerta_enviado")
public class AlertaEnviadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 80)
    private String referencia;

    @Column(name = "enviado_em", nullable = false)
    private Instant enviadoEm;

    protected AlertaEnviadoEntity() {
    }

    public AlertaEnviadoEntity(String tipo, String referencia, Instant enviadoEm) {
        this.tipo = tipo;
        this.referencia = referencia;
        this.enviadoEm = enviadoEm;
    }

    public Long getId() {
        return id;
    }
}
