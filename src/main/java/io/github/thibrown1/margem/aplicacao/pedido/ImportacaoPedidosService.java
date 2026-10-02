package io.github.thibrown1.margem.aplicacao.pedido;

import io.github.thibrown1.margem.aplicacao.porta.PedidoRepositorio;
import io.github.thibrown1.margem.aplicacao.porta.ProdutoRepositorio;
import io.github.thibrown1.margem.dominio.RegraDeNegocioException;
import io.github.thibrown1.margem.dominio.pedido.ItemPedido;
import io.github.thibrown1.margem.dominio.pedido.OrigemPedido;
import io.github.thibrown1.margem.dominio.pedido.Pedido;
import io.github.thibrown1.margem.dominio.pedido.TaxasShopee;
import io.github.thibrown1.margem.dominio.produto.Produto;
import io.github.thibrown1.margem.dominio.taxa.TabelaTaxasShopee;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caso de uso central: transforma pedidos vindos da Shopee em pedidos do sistema.
 * <ul>
 *     <li>É idempotente: importar a mesma planilha duas vezes não duplica nada.</li>
 *     <li>Pedido novo que é venda baixa o estoque; pedido que foi cancelado depois devolve.</li>
 *     <li>O custo de cada item é congelado no momento da venda.</li>
 * </ul>
 */
@Service
public class ImportacaoPedidosService {

    private final PedidoRepositorio pedidos;
    private final ProdutoRepositorio produtos;
    private final TabelaTaxasShopee tabelaTaxas;

    public ImportacaoPedidosService(PedidoRepositorio pedidos, ProdutoRepositorio produtos, TabelaTaxasShopee tabelaTaxas) {
        this.pedidos = pedidos;
        this.produtos = produtos;
        this.tabelaTaxas = tabelaTaxas;
    }

    @Transactional
    public ResultadoImportacao importar(List<PedidoImportado> importados, OrigemPedido origem) {
        Map<String, PedidoImportado> porCodigo = new LinkedHashMap<>();
        importados.forEach(p -> porCodigo.put(p.codigo().trim(), p));

        Map<String, Pedido> existentes = pedidos.buscarPorCodigos(porCodigo.keySet()).stream()
                .collect(Collectors.toMap(Pedido::getCodigo, Function.identity()));
        Set<String> skus = porCodigo.values().stream()
                .flatMap(p -> p.itens().stream())
                .map(item -> Produto.normalizarSku(item.sku()))
                .collect(Collectors.toSet());
        Map<String, Produto> catalogo = produtos.buscarPorSkus(skus).stream()
                .collect(Collectors.toMap(Produto::getSku, Function.identity()));

        Map<String, Produto> produtosAlterados = new LinkedHashMap<>();
        Set<String> skusSemCadastro = new TreeSet<>();
        List<ErroImportacao> erros = new ArrayList<>();
        int novos = 0;
        int atualizados = 0;
        int semAlteracao = 0;
        int comTaxasEstimadas = 0;

        for (PedidoImportado importado : porCodigo.values()) {
            try {
                boolean estimadas = importado.taxas() == null;
                TaxasShopee taxas = estimadas ? estimarTaxas(importado) : importado.taxas();
                Pedido existente = existentes.get(importado.codigo().trim());

                if (existente == null) {
                    Pedido novo = criarPedido(importado, origem, taxas, estimadas, catalogo);
                    if (novo.contaComoVenda()) {
                        movimentarEstoque(novo, -1, catalogo, produtosAlterados);
                    }
                    pedidos.salvar(novo);
                    novos++;
                } else {
                    boolean eraVenda = existente.contaComoVenda();
                    boolean mudou = existente.atualizar(importado.status(), importado.prazoEnvio(),
                            importado.descontoVendedor(), taxas, estimadas);
                    if (!mudou) {
                        semAlteracao++;
                        continue;
                    }
                    if (eraVenda != existente.contaComoVenda()) {
                        movimentarEstoque(existente, existente.contaComoVenda() ? -1 : 1, catalogo, produtosAlterados);
                    }
                    pedidos.salvar(existente);
                    atualizados++;
                }
                if (estimadas) {
                    comTaxasEstimadas++;
                }
                importado.itens().stream()
                        .map(item -> Produto.normalizarSku(item.sku()))
                        .filter(sku -> !catalogo.containsKey(sku))
                        .forEach(skusSemCadastro::add);
            } catch (RegraDeNegocioException e) {
                erros.add(new ErroImportacao(null, importado.codigo(), e.getMessage()));
            }
        }

        produtos.salvarTodos(produtosAlterados.values());
        return new ResultadoImportacao(novos, atualizados, semAlteracao, List.copyOf(skusSemCadastro),
                List.copyOf(erros), avisos(skusSemCadastro, comTaxasEstimadas));
    }

    private Pedido criarPedido(PedidoImportado importado, OrigemPedido origem, TaxasShopee taxas, boolean estimadas,
                               Map<String, Produto> catalogo) {
        List<ItemPedido> itens = importado.itens().stream()
                .map(item -> {
                    Produto produto = catalogo.get(Produto.normalizarSku(item.sku()));
                    return ItemPedido.novo(item.sku(), item.descricao(), item.quantidade(), item.precoUnitario(),
                            produto == null ? null : produto.getCustoUnitario());
                })
                .toList();
        return Pedido.novo(importado.codigo(), importado.status(), origem, importado.criadoEm(), importado.prazoEnvio(),
                importado.descontoVendedor(), taxas, estimadas, itens);
    }

    private TaxasShopee estimarTaxas(PedidoImportado importado) {
        return importado.itens().stream()
                .map(item -> tabelaTaxas.taxasPara(item.precoUnitario(), item.quantidade()))
                .reduce(TaxasShopee.NENHUMA, TaxasShopee::somar);
    }

    /** sinal -1 baixa o estoque (venda); +1 devolve (cancelamento ou devolução). */
    private static void movimentarEstoque(Pedido pedido, int sinal, Map<String, Produto> catalogo,
                                          Map<String, Produto> produtosAlterados) {
        for (ItemPedido item : pedido.getItens()) {
            Produto produto = catalogo.get(item.getSku());
            if (produto == null) {
                continue;
            }
            if (sinal < 0) {
                produto.baixarEstoque(item.getQuantidade());
            } else {
                produto.devolverAoEstoque(item.getQuantidade());
            }
            produtosAlterados.put(produto.getSku(), produto);
        }
    }

    private static List<String> avisos(Set<String> skusSemCadastro, int comTaxasEstimadas) {
        List<String> avisos = new ArrayList<>();
        if (!skusSemCadastro.isEmpty()) {
            avisos.add(skusSemCadastro.size() + " SKU(s) ainda não cadastrado(s). O estoque deles não foi baixado e o lucro"
                    + " desses itens fica pendente até você cadastrar o produto com o custo.");
        }
        if (comTaxasEstimadas > 0) {
            avisos.add(comTaxasEstimadas + " pedido(s) vieram sem as taxas da Shopee. Usei a tabela de taxas configurada"
                    + " para estimar.");
        }
        return List.copyOf(avisos);
    }
}
