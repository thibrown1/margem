package io.github.thibrown1.margem.aplicacao;

import io.github.thibrown1.margem.dominio.RegraDeNegocioException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Intervalo de datas de um relatório, com os dois dias inclusive.
 * Sem datas informadas, vale os últimos 30 dias.
 */
public record Periodo(LocalDate inicio, LocalDate fim) {

    public static final int DIAS_PADRAO = 30;
    public static final int MAXIMO_DE_DIAS = 366;

    public Periodo {
        if (inicio.isAfter(fim)) {
            throw new RegraDeNegocioException("A data inicial não pode ser depois da data final");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) >= MAXIMO_DE_DIAS) {
            throw new RegraDeNegocioException("O período máximo é de " + MAXIMO_DE_DIAS + " dias");
        }
    }

    public static Periodo de(LocalDate inicio, LocalDate fim, Clock relogio) {
        LocalDate hoje = LocalDate.now(relogio);
        LocalDate fimEfetivo = fim != null ? fim : (inicio != null ? inicio.plusDays(DIAS_PADRAO - 1L) : hoje);
        LocalDate inicioEfetivo = inicio != null ? inicio : fimEfetivo.minusDays(DIAS_PADRAO - 1L);
        return new Periodo(inicioEfetivo, fimEfetivo);
    }

    public Instant inicioEm(ZoneId zona) {
        return inicio.atStartOfDay(zona).toInstant();
    }

    /** Primeiro instante depois do período (exclusive). */
    public Instant fimEm(ZoneId zona) {
        return fim.plusDays(1).atStartOfDay(zona).toInstant();
    }
}
