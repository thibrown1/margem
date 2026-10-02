package io.github.thibrown1.margem.adaptador.entrada.web;

import io.github.thibrown1.margem.adaptador.entrada.web.dto.PrecoIdealRequest;
import io.github.thibrown1.margem.adaptador.entrada.web.dto.SimularLucroRequest;
import io.github.thibrown1.margem.adaptador.entrada.web.dto.TabelaTaxasResponse;
import io.github.thibrown1.margem.configuracao.MargemProperties;
import io.github.thibrown1.margem.dominio.Moeda;
import io.github.thibrown1.margem.dominio.lucro.ParametrosLoja;
import io.github.thibrown1.margem.dominio.taxa.FaixaDePrecoAEvitar;
import io.github.thibrown1.margem.dominio.taxa.SimulacaoLucro;
import io.github.thibrown1.margem.dominio.taxa.SimuladorPreco;
import io.github.thibrown1.margem.dominio.taxa.TabelaTaxasShopee;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/simulador")
@Tag(name = "6. Simulador de preço", description = "Considera as taxas da Shopee por faixa de preço, imposto e embalagem.")
public class SimuladorController {

    private final SimuladorPreco simulador;
    private final TabelaTaxasShopee tabela;
    private final ParametrosLoja parametrosLoja;
    private final MargemProperties propriedades;

    public SimuladorController(SimuladorPreco simulador, TabelaTaxasShopee tabela, ParametrosLoja parametrosLoja,
                               MargemProperties propriedades) {
        this.simulador = simulador;
        this.tabela = tabela;
        this.parametrosLoja = parametrosLoja;
        this.propriedades = propriedades;
    }

    @PostMapping("/lucro")
    @Operation(summary = "Vendendo a X, quanto sobra?")
    public SimulacaoLucro simularLucro(@Valid @RequestBody SimularLucroRequest dados) {
        return simulador.simularLucro(dados.precoVenda(), dados.custoProduto());
    }

    @PostMapping("/preco-ideal")
    @Operation(summary = "Por quanto vender para ter Y% de margem?", description = "Devolve o menor preço que atinge a margem.")
    public SimulacaoLucro precoIdeal(@Valid @RequestBody PrecoIdealRequest dados) {
        return simulador.precoIdeal(dados.custoProduto(), dados.margemDesejada());
    }

    @GetMapping("/faixas-a-evitar")
    @Operation(summary = "Faixas de preço em que cobrar mais faz você ganhar menos",
            description = "Quando o preço passa de uma faixa para a próxima, a taxa fixa da Shopee sobe e come o aumento.")
    public List<FaixaDePrecoAEvitar> faixasAEvitar() {
        return simulador.faixasAEvitar();
    }

    @GetMapping("/taxas")
    @Operation(summary = "Tabela de taxas e custos usada nos cálculos")
    public TabelaTaxasResponse taxas() {
        List<TabelaTaxasResponse.Faixa> faixas = tabela.getFaixas().stream()
                .map(faixa -> new TabelaTaxasResponse.Faixa(faixa.precoMinimo(),
                        tabela.limiteSuperior(faixa).map(limite -> limite.subtract(Moeda.UM_CENTAVO)).orElse(null),
                        faixa.comissaoPercentual(), faixa.taxaFixaPorItem()))
                .toList();
        return new TabelaTaxasResponse(propriedades.tabelaTaxas().vigencia(), faixas, tabela.getTaxaAdicionalPorItem(),
                parametrosLoja.custoEmbalagemPorPedido(), parametrosLoja.aliquotaImposto());
    }
}
