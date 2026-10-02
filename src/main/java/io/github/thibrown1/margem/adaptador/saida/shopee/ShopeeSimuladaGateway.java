package io.github.thibrown1.margem.adaptador.saida.shopee;

import io.github.thibrown1.margem.aplicacao.pedido.ItemImportado;
import io.github.thibrown1.margem.aplicacao.pedido.PedidoImportado;
import io.github.thibrown1.margem.aplicacao.porta.ShopeeGateway;
import io.github.thibrown1.margem.dominio.Moeda;
import io.github.thibrown1.margem.dominio.pedido.StatusPedido;
import io.github.thibrown1.margem.dominio.pedido.TaxasShopee;
import io.github.thibrown1.margem.dominio.taxa.TabelaTaxasShopee;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Shopee de mentira para demonstração e desenvolvimento, sem precisar de credenciais.
 * <p>
 * Os pedidos são gerados de forma determinística a partir da data (mesma data, mesmos pedidos),
 * então sincronizar várias vezes não duplica nada. O status evolui com o tempo, como na loja real:
 * pedido de hoje está "a enviar", de alguns dias atrás está "enviado" e os mais antigos, "concluídos".
 * As taxas seguem a mesma tabela configurada para o simulador de preços.
 */
public class ShopeeSimuladaGateway implements ShopeeGateway {

    record ProdutoSimulado(String sku, String nome, BigDecimal preco, int popularidade) {
    }

    /** Os seis primeiros existem nos dados de demonstração; a capa de notebook, não (para mostrar SKU sem cadastro). */
    static final List<ProdutoSimulado> CATALOGO = List.of(
            new ProdutoSimulado("GARRAFA-INOX-500", "Garrafa Térmica Inox 500ml", new BigDecimal("59.90"), 30),
            new ProdutoSimulado("KIT-POTES-3", "Kit 3 Potes Herméticos de Vidro", new BigDecimal("49.90"), 18),
            new ProdutoSimulado("ORGANIZADOR-GAVETA", "Organizador de Gaveta 8 Divisórias", new BigDecimal("24.90"), 24),
            new ProdutoSimulado("SUPORTE-CELULAR", "Suporte de Celular Articulado", new BigDecimal("19.90"), 20),
            new ProdutoSimulado("LUMINARIA-LED", "Luminária LED de Mesa Recarregável", new BigDecimal("84.90"), 10),
            new ProdutoSimulado("CAFETEIRA-ITALIANA", "Cafeteira Italiana 6 Xícaras", new BigDecimal("129.90"), 8),
            new ProdutoSimulado("CAPA-NOTEBOOK-15", "Capa para Notebook 15,6 polegadas", new BigDecimal("69.90"), 5));

    private static final DateTimeFormatter DATA_DO_CODIGO = DateTimeFormatter.ofPattern("yyMMdd");
    private static final BigDecimal CUPOM = new BigDecimal("5.00");
    private static final int POPULARIDADE_TOTAL = CATALOGO.stream().mapToInt(ProdutoSimulado::popularidade).sum();

    private final TabelaTaxasShopee tabela;
    private final Clock relogio;
    private final ZoneId zona;

    public ShopeeSimuladaGateway(TabelaTaxasShopee tabela, Clock relogio) {
        this.tabela = tabela;
        this.relogio = relogio;
        this.zona = relogio.getZone();
    }

    @Override
    public List<PedidoImportado> buscarPedidos(Instant de, Instant ate) {
        Instant agora = relogio.instant();
        Instant limite = ate.isAfter(agora) ? agora : ate;
        List<PedidoImportado> pedidos = new ArrayList<>();
        LocalDate ultimoDia = limite.atZone(zona).toLocalDate();
        for (LocalDate dia = de.atZone(zona).toLocalDate(); !dia.isAfter(ultimoDia); dia = dia.plusDays(1)) {
            Random aleatorio = new Random(dia.toEpochDay() * 7_919L + 17);
            int pedidosNoDia = 2 + aleatorio.nextInt(5);
            for (int numero = 1; numero <= pedidosNoDia; numero++) {
                PedidoImportado pedido = gerar(dia, numero, aleatorio, agora);
                if (!pedido.criadoEm().isBefore(de) && pedido.criadoEm().isBefore(limite)) {
                    pedidos.add(pedido);
                }
            }
        }
        return pedidos;
    }

    @Override
    public String descricao() {
        return "Shopee simulada";
    }

    /** Todos os sorteios acontecem antes de olhar o relógio, para o mesmo dia gerar sempre os mesmos pedidos. */
    private PedidoImportado gerar(LocalDate dia, int numero, Random aleatorio, Instant agora) {
        Instant criadoEm = dia.atTime(8 + aleatorio.nextInt(14), aleatorio.nextInt(60)).atZone(zona).toInstant();

        int produtosDiferentes = aleatorio.nextInt(100) < 20 ? 2 : 1;
        List<ItemImportado> itens = new ArrayList<>();
        Set<String> escolhidos = new HashSet<>();
        for (int i = 0; i < produtosDiferentes; i++) {
            ProdutoSimulado produto = sortear(aleatorio);
            int quantidade = aleatorio.nextInt(100) < 15 ? 2 : 1;
            if (escolhidos.add(produto.sku())) {
                itens.add(new ItemImportado(produto.sku(), produto.nome(), quantidade, produto.preco()));
            }
        }
        boolean cancelado = aleatorio.nextInt(100) < 6;
        boolean esquecidoNaExpedicao = aleatorio.nextInt(100) < 5;
        boolean usouCupom = aleatorio.nextInt(100) < 12;

        Duration idade = Duration.between(criadoEm, agora);
        StatusPedido status;
        if (cancelado) {
            status = StatusPedido.CANCELADO;
        } else if (idade.compareTo(Duration.ofHours(36)) < 0 || (esquecidoNaExpedicao && idade.compareTo(Duration.ofDays(3)) < 0)) {
            status = StatusPedido.AGUARDANDO_ENVIO;
        } else if (idade.compareTo(Duration.ofDays(4)) < 0) {
            status = StatusPedido.ENVIADO;
        } else {
            status = StatusPedido.CONCLUIDO;
        }

        TaxasShopee taxas = status == StatusPedido.CANCELADO
                ? TaxasShopee.NENHUMA
                : itens.stream()
                .map(item -> tabela.taxasPara(item.precoUnitario(), item.quantidade()))
                .reduce(TaxasShopee.NENHUMA, TaxasShopee::somar);

        return new PedidoImportado(
                "SIM" + DATA_DO_CODIGO.format(dia) + String.format("%03d", numero),
                status,
                criadoEm,
                criadoEm.plus(Duration.ofDays(2)),
                usouCupom ? CUPOM : Moeda.ZERO,
                taxas,
                List.copyOf(itens));
    }

    private static ProdutoSimulado sortear(Random aleatorio) {
        int sorteio = aleatorio.nextInt(POPULARIDADE_TOTAL);
        for (ProdutoSimulado produto : CATALOGO) {
            sorteio -= produto.popularidade();
            if (sorteio < 0) {
                return produto;
            }
        }
        return CATALOGO.getFirst();
    }
}
