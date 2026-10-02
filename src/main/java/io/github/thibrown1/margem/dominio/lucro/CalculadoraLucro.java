package io.github.thibrown1.margem.dominio.lucro;

import io.github.thibrown1.margem.dominio.Moeda;
import io.github.thibrown1.margem.dominio.pedido.ItemPedido;
import io.github.thibrown1.margem.dominio.pedido.Pedido;
import io.github.thibrown1.margem.dominio.pedido.TaxasShopee;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula o lucro real de um pedido. É a regra central do sistema e não depende de Spring,
 * banco ou Shopee: recebe um pedido e devolve a conta aberta, item por item.
 */
public class CalculadoraLucro {

    private final ParametrosLoja parametros;

    public CalculadoraLucro(ParametrosLoja parametros) {
        this.parametros = parametros;
    }

    public LucroPedido calcular(Pedido pedido) {
        List<ItemPedido> itens = pedido.getItens();
        TaxasShopee taxas = pedido.getTaxas();

        BigDecimal valorBruto = pedido.valorBruto();
        BigDecimal desconto = pedido.getDescontoVendedor();
        BigDecimal totalTaxas = taxas.total();
        BigDecimal valorRecebido = Moeda.arredondar(valorBruto.subtract(desconto).subtract(totalTaxas));

        BigDecimal baseDaVenda = Moeda.maximo(valorBruto.subtract(desconto), Moeda.ZERO);
        BigDecimal impostos = Moeda.percentualDe(baseDaVenda, parametros.aliquotaImposto());
        BigDecimal embalagem = parametros.custoEmbalagemPorPedido();

        List<BigDecimal> pesos = itens.stream().map(ItemPedido::subtotal).toList();
        List<BigDecimal> descontosPorItem = Rateio.proporcional(desconto, pesos);
        List<BigDecimal> taxasPorItem = Rateio.proporcional(totalTaxas, pesos);
        List<BigDecimal> embalagemPorItem = Rateio.proporcional(embalagem, pesos);
        List<BigDecimal> impostosPorItem = Rateio.proporcional(impostos, pesos);

        List<LucroItem> lucroDosItens = new ArrayList<>(itens.size());
        BigDecimal custoProdutos = BigDecimal.ZERO;
        for (int i = 0; i < itens.size(); i++) {
            ItemPedido item = itens.get(i);
            BigDecimal receita = Moeda.arredondar(item.subtotal());
            BigDecimal custo = Moeda.arredondar(item.custoTotal());
            BigDecimal lucroItem = receita
                    .subtract(descontosPorItem.get(i))
                    .subtract(taxasPorItem.get(i))
                    .subtract(custo)
                    .subtract(embalagemPorItem.get(i))
                    .subtract(impostosPorItem.get(i));
            lucroDosItens.add(new LucroItem(item.getSku(), item.getDescricao(), item.getQuantidade(), receita,
                    descontosPorItem.get(i), taxasPorItem.get(i), custo, embalagemPorItem.get(i),
                    impostosPorItem.get(i), Moeda.arredondar(lucroItem), item.custoPendente()));
            custoProdutos = custoProdutos.add(custo);
        }

        BigDecimal lucro = Moeda.arredondar(valorRecebido.subtract(custoProdutos).subtract(embalagem).subtract(impostos));

        return new LucroPedido(
                pedido.getCodigo(),
                valorBruto,
                desconto,
                taxas.comissao(),
                taxas.servico(),
                taxas.transacao(),
                Moeda.arredondar(totalTaxas),
                pedido.isTaxasEstimadas(),
                valorRecebido,
                Moeda.arredondar(custoProdutos),
                embalagem,
                impostos,
                lucro,
                Moeda.participacao(lucro, baseDaVenda),
                pedido.temCustoPendente(),
                List.copyOf(lucroDosItens));
    }
}
