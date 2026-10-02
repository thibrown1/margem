package io.github.thibrown1.margem.aplicacao.relatorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResumoFinanceiro(
        LocalDate inicio,
        LocalDate fim,
        int pedidos,
        int pedidosCanceladosOuDevolvidos,
        int itensVendidos,
        BigDecimal faturamentoBruto,
        BigDecimal descontosDoVendedor,
        BigDecimal taxasShopee,
        BigDecimal valorRecebido,
        BigDecimal custoProdutos,
        BigDecimal custoEmbalagens,
        BigDecimal impostos,
        BigDecimal lucroLiquido,
        BigDecimal margemPercentual,
        BigDecimal ticketMedio,
        BigDecimal lucroMedioPorPedido,
        List<ComposicaoItem> paraOndeFoiODinheiro,
        List<String> avisos) {
}
