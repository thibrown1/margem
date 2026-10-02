package io.github.thibrown1.margem.adaptador.entrada.web;

import io.github.thibrown1.margem.aplicacao.relatorio.LucroPorProduto;
import io.github.thibrown1.margem.aplicacao.relatorio.OrdenacaoProdutos;
import io.github.thibrown1.margem.aplicacao.relatorio.RelatorioService;
import io.github.thibrown1.margem.aplicacao.relatorio.ResumoFinanceiro;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/relatorios")
@Tag(name = "5. Relatórios", description = "Sem datas, os relatórios mostram os últimos 30 dias.")
public class RelatorioController {

    private final RelatorioService relatorios;

    public RelatorioController(RelatorioService relatorios) {
        this.relatorios = relatorios;
    }

    @GetMapping("/resumo")
    @Operation(summary = "Faturamento, taxas, custos e lucro do período, e para onde foi cada real vendido")
    public ResumoFinanceiro resumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return relatorios.resumo(inicio, fim);
    }

    @GetMapping("/produtos")
    @Operation(summary = "Lucro por produto", description = "O mais vendido nem sempre é o que mais dá lucro.")
    public List<LucroPorProduto> porProduto(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "LUCRO") OrdenacaoProdutos ordenarPor) {
        return relatorios.lucroPorProduto(inicio, fim, ordenarPor);
    }

    @GetMapping("/prejuizo")
    @Operation(summary = "Produtos que estão sendo vendidos no prejuízo")
    public List<LucroPorProduto> prejuizo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return relatorios.produtosNoPrejuizo(inicio, fim);
    }
}
