package io.github.thibrown1.margem.aplicacao.relatorio;

import io.github.thibrown1.margem.aplicacao.Periodo;
import io.github.thibrown1.margem.aplicacao.porta.PedidoRepositorio;
import io.github.thibrown1.margem.dominio.Moeda;
import io.github.thibrown1.margem.dominio.lucro.CalculadoraLucro;
import io.github.thibrown1.margem.dominio.lucro.LucroItem;
import io.github.thibrown1.margem.dominio.lucro.LucroPedido;
import io.github.thibrown1.margem.dominio.pedido.Pedido;
import io.github.thibrown1.margem.dominio.pedido.StatusPedido;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Relatórios financeiros. O lucro é calculado em Java pela {@link CalculadoraLucro} (e não em SQL)
 * para que o relatório use exatamente a mesma regra da tela de cada pedido.
 */
@Service
@Transactional(readOnly = true)
public class RelatorioService {

    private final PedidoRepositorio pedidos;
    private final CalculadoraLucro calculadora;
    private final Clock relogio;
    private final ZoneId zona;

    public RelatorioService(PedidoRepositorio pedidos, CalculadoraLucro calculadora, Clock relogio) {
        this.pedidos = pedidos;
        this.calculadora = calculadora;
        this.relogio = relogio;
        this.zona = relogio.getZone();
    }

    public ResumoFinanceiro resumo(LocalDate inicio, LocalDate fim) {
        Periodo periodo = Periodo.de(inicio, fim, relogio);
        List<Pedido> doPeriodo = pedidos.buscarCriadosEntre(periodo.inicioEm(zona), periodo.fimEm(zona));
        List<LucroPedido> vendas = doPeriodo.stream().filter(Pedido::contaComoVenda).map(calculadora::calcular).toList();

        int cancelados = (int) doPeriodo.stream().map(Pedido::getStatus).filter(StatusPedido::foiDesfeito).count();
        int itensVendidos = doPeriodo.stream().filter(Pedido::contaComoVenda).mapToInt(Pedido::quantidadeDeItens).sum();

        BigDecimal faturamento = somar(vendas, LucroPedido::valorBruto);
        BigDecimal descontos = somar(vendas, LucroPedido::descontoVendedor);
        BigDecimal taxas = somar(vendas, LucroPedido::totalTaxasShopee);
        BigDecimal recebido = somar(vendas, LucroPedido::valorRecebido);
        BigDecimal custoProdutos = somar(vendas, LucroPedido::custoProdutos);
        BigDecimal embalagens = somar(vendas, LucroPedido::custoEmbalagem);
        BigDecimal impostos = somar(vendas, LucroPedido::impostos);
        BigDecimal lucro = somar(vendas, LucroPedido::lucro);
        BigDecimal quantidade = BigDecimal.valueOf(Math.max(vendas.size(), 1));

        List<ComposicaoItem> composicao = List.of(
                new ComposicaoItem("Taxas da Shopee", taxas, Moeda.participacao(taxas, faturamento)),
                new ComposicaoItem("Custo dos produtos", custoProdutos, Moeda.participacao(custoProdutos, faturamento)),
                new ComposicaoItem("Impostos", impostos, Moeda.participacao(impostos, faturamento)),
                new ComposicaoItem("Cupons do vendedor", descontos, Moeda.participacao(descontos, faturamento)),
                new ComposicaoItem("Embalagens", embalagens, Moeda.participacao(embalagens, faturamento)),
                new ComposicaoItem("Lucro", lucro, Moeda.participacao(lucro, faturamento)));

        return new ResumoFinanceiro(periodo.inicio(), periodo.fim(), vendas.size(), cancelados, itensVendidos,
                faturamento, descontos, taxas, recebido, custoProdutos, embalagens, impostos, lucro,
                Moeda.participacao(lucro, faturamento.subtract(descontos)),
                Moeda.arredondar(faturamento.divide(quantidade, Moeda.ESCALA, Moeda.ARREDONDAMENTO)),
                Moeda.arredondar(lucro.divide(quantidade, Moeda.ESCALA, Moeda.ARREDONDAMENTO)),
                composicao, avisos(vendas));
    }

    public List<LucroPorProduto> lucroPorProduto(LocalDate inicio, LocalDate fim, OrdenacaoProdutos ordenacao) {
        Periodo periodo = Periodo.de(inicio, fim, relogio);
        List<LucroItem> itens = pedidos.buscarCriadosEntre(periodo.inicioEm(zona), periodo.fimEm(zona)).stream()
                .filter(Pedido::contaComoVenda)
                .map(calculadora::calcular)
                .flatMap(lucro -> lucro.itens().stream())
                .toList();

        Map<String, List<LucroItem>> porSku = new LinkedHashMap<>();
        itens.forEach(item -> porSku.computeIfAbsent(item.sku(), sku -> new ArrayList<>()).add(item));

        OrdenacaoProdutos ordem = ordenacao == null ? OrdenacaoProdutos.LUCRO : ordenacao;
        return porSku.values().stream()
                .map(RelatorioService::consolidar)
                .sorted(ordem.comparador())
                .toList();
    }

    public List<LucroPorProduto> produtosNoPrejuizo(LocalDate inicio, LocalDate fim) {
        return lucroPorProduto(inicio, fim, OrdenacaoProdutos.LUCRO).stream()
                .filter(produto -> produto.lucro().signum() < 0)
                .sorted((a, b) -> a.lucro().compareTo(b.lucro()))
                .toList();
    }

    private static LucroPorProduto consolidar(List<LucroItem> vendas) {
        LucroItem primeiro = vendas.getFirst();
        int quantidade = vendas.stream().mapToInt(LucroItem::quantidade).sum();
        BigDecimal faturamento = somarItens(vendas, LucroItem::receita);
        BigDecimal descontos = somarItens(vendas, LucroItem::descontoVendedor);
        BigDecimal taxas = somarItens(vendas, LucroItem::taxasShopee);
        BigDecimal custo = somarItens(vendas, LucroItem::custoProdutos);
        BigDecimal lucro = somarItens(vendas, LucroItem::lucro);
        boolean pendente = vendas.stream().anyMatch(LucroItem::custoPendente);
        return new LucroPorProduto(primeiro.sku(), primeiro.descricao(), quantidade, faturamento, taxas, custo, lucro,
                Moeda.participacao(lucro, faturamento.subtract(descontos)),
                lucro.divide(BigDecimal.valueOf(quantidade), Moeda.ESCALA, Moeda.ARREDONDAMENTO),
                pendente);
    }

    private static List<String> avisos(List<LucroPedido> vendas) {
        List<LucroPedido> pendentes = vendas.stream().filter(LucroPedido::custoPendente).toList();
        List<String> avisos = new ArrayList<>();
        if (!pendentes.isEmpty()) {
            TreeSet<String> skus = new TreeSet<>();
            pendentes.forEach(p -> p.itens().stream().filter(LucroItem::custoPendente).map(LucroItem::sku).forEach(skus::add));
            avisos.add(pendentes.size() + " pedido(s) têm produtos sem custo cadastrado (" + String.join(", ", skus)
                    + "). O lucro desses pedidos está maior do que o real até você cadastrar esses SKUs.");
        }
        long estimados = vendas.stream().filter(LucroPedido::taxasEstimadas).count();
        if (estimados > 0) {
            avisos.add(estimados + " pedido(s) estão com taxas estimadas pela tabela, não com o valor cobrado pela Shopee.");
        }
        return List.copyOf(avisos);
    }

    private static BigDecimal somar(List<LucroPedido> lucros, Function<LucroPedido, BigDecimal> campo) {
        return Moeda.somar(lucros.stream().map(campo).toList());
    }

    private static BigDecimal somarItens(List<LucroItem> itens, Function<LucroItem, BigDecimal> campo) {
        return Moeda.somar(itens.stream().map(campo).toList());
    }
}
