package io.github.thibrown1.margem.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.Locale;

/**
 * Regras de arredondamento e formatação de dinheiro usadas no sistema inteiro.
 * Todo valor monetário é BigDecimal com 2 casas; double nunca entra em conta de dinheiro.
 */
public final class Moeda {

    public static final int ESCALA = 2;
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(ESCALA);
    public static final BigDecimal UM_CENTAVO = new BigDecimal("0.01");

    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private Moeda() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor == null ? null : valor.setScale(ESCALA, ARREDONDAMENTO);
    }

    public static BigDecimal ouZero(BigDecimal valor) {
        return valor == null ? ZERO : arredondar(valor);
    }

    /** Aplica um percentual (ex.: 20 = 20%) sobre um valor, já arredondado em centavos. */
    public static BigDecimal percentualDe(BigDecimal valor, BigDecimal percentual) {
        return arredondar(valor.multiply(percentual).divide(CEM, 10, ARREDONDAMENTO));
    }

    /** Quanto a parte representa do total, em percentual com 2 casas. Total zero resulta em zero. */
    public static BigDecimal participacao(BigDecimal parte, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return ZERO;
        }
        return parte.multiply(CEM).divide(total, ESCALA, ARREDONDAMENTO);
    }

    public static BigDecimal somar(Collection<BigDecimal> valores) {
        return arredondar(valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public static BigDecimal maximo(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    /** Formata no padrão brasileiro: R$ 1.234,56 ou -R$ 4,28. */
    public static String formatar(BigDecimal valor) {
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(PT_BR));
        String texto = formato.format(arredondar(valor).abs());
        return (valor.signum() < 0 ? "-R$ " : "R$ ") + texto;
    }
}
