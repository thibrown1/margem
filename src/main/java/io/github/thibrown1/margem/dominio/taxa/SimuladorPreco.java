package io.github.thibrown1.margem.dominio.taxa;

import io.github.thibrown1.margem.dominio.Moeda;
import io.github.thibrown1.margem.dominio.RegraDeNegocioException;
import io.github.thibrown1.margem.dominio.lucro.ParametrosLoja;
import io.github.thibrown1.margem.dominio.pedido.TaxasShopee;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Responde às duas perguntas de quem vai anunciar um produto:
 * "vendendo a X, quanto sobra?" e "por quanto eu preciso vender para ter Y% de margem?".
 * A simulação considera 1 unidade por pedido (a embalagem entra inteira).
 */
public class SimuladorPreco {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final int LIMITE_DE_PASSOS = 200_000;

    private final TabelaTaxasShopee tabela;
    private final ParametrosLoja parametros;
    private final List<FaixaDePrecoAEvitar> faixasAEvitar;

    public SimuladorPreco(TabelaTaxasShopee tabela, ParametrosLoja parametros) {
        this.tabela = tabela;
        this.parametros = parametros;
        this.faixasAEvitar = calcularFaixasAEvitar();
    }

    public SimulacaoLucro simularLucro(BigDecimal precoVenda, BigDecimal custoProduto) {
        if (precoVenda == null || precoVenda.signum() <= 0) {
            throw new RegraDeNegocioException("O preço de venda deve ser maior que zero");
        }
        if (custoProduto == null || custoProduto.signum() < 0) {
            throw new RegraDeNegocioException("O custo do produto não pode ser negativo");
        }
        BigDecimal preco = Moeda.arredondar(precoVenda);
        BigDecimal custo = Moeda.arredondar(custoProduto);

        FaixaTaxa faixa = tabela.faixaPara(preco);
        TaxasShopee taxas = tabela.taxasPara(preco, 1);
        BigDecimal impostos = Moeda.percentualDe(preco, parametros.aliquotaImposto());
        BigDecimal embalagem = parametros.custoEmbalagemPorPedido();
        BigDecimal valorRecebido = preco.subtract(taxas.total());
        BigDecimal lucro = valorRecebido.subtract(impostos).subtract(embalagem).subtract(custo);

        String alerta = faixasAEvitar.stream()
                .filter(f -> f.contem(preco))
                .findFirst()
                .map(f -> "Vendendo a " + Moeda.formatar(preco) + " você lucra menos do que vendendo a "
                        + Moeda.formatar(f.precoMelhor()) + ". " + f.motivo())
                .orElse(null);

        return new SimulacaoLucro(preco, faixa.comissaoPercentual(), taxas.comissao(), taxas.servico(), valorRecebido,
                impostos, embalagem, custo, lucro, Moeda.participacao(lucro, preco), alerta);
    }

    /** Menor preço que entrega a margem desejada, considerando que cada faixa tem comissão e taxa fixa próprias. */
    public SimulacaoLucro precoIdeal(BigDecimal custoProduto, BigDecimal margemDesejada) {
        if (margemDesejada == null || margemDesejada.signum() < 0 || margemDesejada.compareTo(CEM) >= 0) {
            throw new RegraDeNegocioException("A margem desejada deve estar entre 0 e 100%");
        }
        if (custoProduto == null || custoProduto.signum() < 0) {
            throw new RegraDeNegocioException("O custo do produto não pode ser negativo");
        }
        BigDecimal custosQueNaoDependemDoPreco = custoProduto.add(parametros.custoEmbalagemPorPedido());

        BigDecimal melhorPreco = null;
        for (FaixaTaxa faixa : tabela.getFaixas()) {
            BigDecimal percentuais = faixa.comissaoPercentual().add(parametros.aliquotaImposto()).add(margemDesejada);
            BigDecimal sobraPorReal = BigDecimal.ONE.subtract(percentuais.divide(CEM, 10, RoundingMode.HALF_UP));
            if (sobraPorReal.signum() <= 0) {
                continue;
            }
            BigDecimal numerador = custosQueNaoDependemDoPreco.add(faixa.taxaFixaPorItem()).add(tabela.getTaxaAdicionalPorItem());
            BigDecimal preco = Moeda.maximo(numerador.divide(sobraPorReal, Moeda.ESCALA, RoundingMode.CEILING), faixa.precoMinimo());
            if (preco.signum() <= 0) {
                preco = Moeda.UM_CENTAVO;
            }
            Optional<BigDecimal> limite = tabela.limiteSuperior(faixa);

            // A conta acima ignora o arredondamento das taxas em centavos. Ajusta centavo a centavo até achar
            // o menor preço que realmente atinge a margem.
            int passos = 0;
            while (!atingeMargem(preco, custoProduto, margemDesejada) && passos++ < 1_000) {
                preco = preco.add(Moeda.UM_CENTAVO);
            }
            passos = 0;
            while (passos++ < 1_000) {
                BigDecimal umCentavoAMenos = preco.subtract(Moeda.UM_CENTAVO);
                if (umCentavoAMenos.signum() <= 0 || umCentavoAMenos.compareTo(faixa.precoMinimo()) < 0
                        || !atingeMargem(umCentavoAMenos, custoProduto, margemDesejada)) {
                    break;
                }
                preco = umCentavoAMenos;
            }
            BigDecimal candidato = preco;
            boolean dentroDaFaixa = limite.map(l -> candidato.compareTo(l) < 0).orElse(true);
            if (candidato.signum() > 0 && dentroDaFaixa && (melhorPreco == null || candidato.compareTo(melhorPreco) < 0)) {
                melhorPreco = candidato;
            }
        }

        if (melhorPreco == null) {
            throw new MargemInviavelException("Com as taxas atuais não existe preço que dê " + margemDesejada.stripTrailingZeros().toPlainString()
                    + "% de margem. Tente uma margem menor.");
        }
        return simularLucro(melhorPreco, custoProduto);
    }

    public List<FaixaDePrecoAEvitar> faixasAEvitar() {
        return faixasAEvitar;
    }

    /** Compara sem arredondar a margem: lucro / preço >= margem desejada. */
    private boolean atingeMargem(BigDecimal preco, BigDecimal custoProduto, BigDecimal margemDesejada) {
        BigDecimal lucro = simularLucro(preco, custoProduto).lucro();
        return lucro.multiply(CEM).compareTo(margemDesejada.multiply(preco)) >= 0;
    }

    /** O que sobra de cada venda antes de pagar produto e embalagem (que não mudam com o preço). */
    private BigDecimal sobraAntesDosCustos(BigDecimal preco) {
        return preco
                .subtract(tabela.taxasPara(preco, 1).total())
                .subtract(Moeda.percentualDe(preco, parametros.aliquotaImposto()));
    }

    private List<FaixaDePrecoAEvitar> calcularFaixasAEvitar() {
        List<FaixaTaxa> faixas = tabela.getFaixas();
        List<FaixaDePrecoAEvitar> resultado = new ArrayList<>();

        for (int i = 1; i < faixas.size(); i++) {
            FaixaTaxa anterior = faixas.get(i - 1);
            FaixaTaxa atual = faixas.get(i);
            BigDecimal ultimoPrecoDaFaixaAnterior = atual.precoMinimo().subtract(Moeda.UM_CENTAVO);
            if (ultimoPrecoDaFaixaAnterior.compareTo(anterior.precoMinimo()) < 0 || ultimoPrecoDaFaixaAnterior.signum() <= 0) {
                continue;
            }
            BigDecimal referencia = sobraAntesDosCustos(ultimoPrecoDaFaixaAnterior);
            BigDecimal inicio = atual.precoMinimo();
            if (sobraAntesDosCustos(inicio).compareTo(referencia) > 0) {
                continue;
            }

            Optional<BigDecimal> limite = tabela.limiteSuperior(atual);
            BigDecimal preco = inicio;
            int passos = 0;
            while (sobraAntesDosCustos(preco).compareTo(referencia) <= 0 && passos++ < LIMITE_DE_PASSOS) {
                preco = preco.add(Moeda.UM_CENTAVO);
                if (limite.isPresent() && preco.compareTo(limite.get()) >= 0) {
                    break;
                }
            }
            BigDecimal fim = preco.subtract(Moeda.UM_CENTAVO);
            resultado.add(new FaixaDePrecoAEvitar(inicio, fim, ultimoPrecoDaFaixaAnterior, explicar(anterior, atual)));
        }
        return List.copyOf(resultado);
    }

    private static String explicar(FaixaTaxa anterior, FaixaTaxa atual) {
        String aPartirDe = "A partir de " + Moeda.formatar(atual.precoMinimo());
        if (atual.taxaFixaPorItem().compareTo(anterior.taxaFixaPorItem()) > 0) {
            return aPartirDe + " a taxa fixa por item sobe de " + Moeda.formatar(anterior.taxaFixaPorItem())
                    + " para " + Moeda.formatar(atual.taxaFixaPorItem()) + ".";
        }
        return aPartirDe + " a comissão sobe de " + anterior.comissaoPercentual().stripTrailingZeros().toPlainString()
                + "% para " + atual.comissaoPercentual().stripTrailingZeros().toPlainString() + "%.";
    }
}
