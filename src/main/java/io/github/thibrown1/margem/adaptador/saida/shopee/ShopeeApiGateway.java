package io.github.thibrown1.margem.adaptador.saida.shopee;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.thibrown1.margem.aplicacao.pedido.ItemImportado;
import io.github.thibrown1.margem.aplicacao.pedido.PedidoImportado;
import io.github.thibrown1.margem.aplicacao.porta.ShopeeGateway;
import io.github.thibrown1.margem.dominio.Moeda;
import io.github.thibrown1.margem.dominio.pedido.StatusPedido;
import io.github.thibrown1.margem.dominio.pedido.TaxasShopee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adaptador para a Shopee Open Platform v2.
 * <p>
 * Fluxo: lista os códigos dos pedidos (em janelas de até 15 dias, paginando pelo cursor), busca os detalhes
 * em lotes de 50 e, para pedidos que viraram venda, busca o repasse (escrow) com as taxas cobradas.
 * Campos de endereço e dados do comprador nunca são solicitados.
 * <p>
 * Quando a Shopee recusa o access_token, o adaptador renova o token com o refresh_token e tenta de novo uma vez.
 */
public class ShopeeApiGateway implements ShopeeGateway {

    static final String PATH_LISTA_PEDIDOS = "/api/v2/order/get_order_list";
    static final String PATH_DETALHES_PEDIDOS = "/api/v2/order/get_order_detail";
    static final String PATH_REPASSE = "/api/v2/payment/get_escrow_detail";
    static final String PATH_RENOVAR_TOKEN = "/api/v2/auth/access_token/get";
    static final Duration JANELA_MAXIMA = Duration.ofDays(15);
    static final int TAMANHO_DA_PAGINA = 100;
    static final int LOTE_DE_DETALHES = 50;

    private static final Logger log = LoggerFactory.getLogger(ShopeeApiGateway.class);
    private static final int LIMITE_DE_PAGINAS = 1_000;

    private final RestClient http;
    private final long partnerId;
    private final String partnerKey;
    private final long shopId;
    private final Clock relogio;
    private volatile String accessToken;
    private volatile String refreshToken;

    public ShopeeApiGateway(RestClient http, long partnerId, String partnerKey, long shopId,
                            String accessToken, String refreshToken, Clock relogio) {
        this.http = http;
        this.partnerId = partnerId;
        this.partnerKey = partnerKey;
        this.shopId = shopId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.relogio = relogio;
    }

    @Override
    public List<PedidoImportado> buscarPedidos(Instant de, Instant ate) {
        List<String> codigos = new ArrayList<>();
        for (Instant inicio = de; inicio.isBefore(ate); ) {
            Instant fim = inicio.plus(JANELA_MAXIMA).isBefore(ate) ? inicio.plus(JANELA_MAXIMA) : ate;
            codigos.addAll(listarCodigos(inicio, fim));
            inicio = fim;
        }

        List<PedidoImportado> pedidos = new ArrayList<>();
        for (int i = 0; i < codigos.size(); i += LOTE_DE_DETALHES) {
            List<String> lote = codigos.subList(i, Math.min(i + LOTE_DE_DETALHES, codigos.size()));
            Map<String, String> parametros = new LinkedHashMap<>();
            parametros.put("order_sn_list", String.join(",", lote));
            parametros.put("response_optional_fields", "item_list");
            JsonNode resposta = chamarLoja(PATH_DETALHES_PEDIDOS, parametros);
            resposta.path("order_list").forEach(pedido -> converter(pedido).ifPresent(pedidos::add));
        }
        return pedidos;
    }

    @Override
    public String descricao() {
        return "API da Shopee (loja " + shopId + ")";
    }

    private List<String> listarCodigos(Instant de, Instant ate) {
        List<String> codigos = new ArrayList<>();
        String cursor = "";
        boolean temMais = true;
        int paginas = 0;
        while (temMais && paginas++ < LIMITE_DE_PAGINAS) {
            Map<String, String> parametros = new LinkedHashMap<>();
            parametros.put("time_range_field", "create_time");
            parametros.put("time_from", String.valueOf(de.getEpochSecond()));
            parametros.put("time_to", String.valueOf(ate.getEpochSecond()));
            parametros.put("page_size", String.valueOf(TAMANHO_DA_PAGINA));
            parametros.put("cursor", cursor);
            JsonNode resposta = chamarLoja(PATH_LISTA_PEDIDOS, parametros);
            resposta.path("order_list").forEach(pedido -> codigos.add(pedido.path("order_sn").asText()));
            temMais = resposta.path("more").asBoolean(false);
            cursor = resposta.path("next_cursor").asText("");
        }
        return codigos;
    }

    private Optional<PedidoImportado> converter(JsonNode pedido) {
        String codigo = pedido.path("order_sn").asText();
        String codigoStatus = pedido.path("order_status").asText();
        Optional<StatusPedido> status = StatusShopee.doCodigoDaApi(codigoStatus);
        if (status.isEmpty()) {
            log.warn("Pedido {} ignorado: status desconhecido '{}'", codigo, codigoStatus);
            return Optional.empty();
        }

        List<ItemImportado> itens = new ArrayList<>();
        for (JsonNode item : pedido.path("item_list")) {
            String nome = item.path("item_name").asText("");
            String variacao = item.path("model_name").asText("");
            BigDecimal preco = decimal(item, "model_discounted_price");
            if (preco.signum() == 0) {
                preco = decimal(item, "model_original_price");
            }
            itens.add(new ItemImportado(
                    IdentificadorDeProduto.escolher(item.path("model_sku").asText(""), item.path("item_sku").asText(""), nome, variacao),
                    IdentificadorDeProduto.descricao(nome, variacao),
                    item.path("model_quantity_purchased").asInt(1),
                    preco));
        }

        TaxasShopee taxas = null;
        BigDecimal cupomDoVendedor = Moeda.ZERO;
        if (status.get().contaComoVenda()) {
            Optional<JsonNode> renda = buscarRepasse(codigo).map(repasse -> repasse.path("order_income"));
            if (renda.isPresent() && !renda.get().isMissingNode()) {
                taxas = new TaxasShopee(decimal(renda.get(), "commission_fee"), decimal(renda.get(), "service_fee"),
                        decimal(renda.get(), "seller_transaction_fee"));
                cupomDoVendedor = decimal(renda.get(), "voucher_from_seller");
            }
        }

        long prazo = pedido.path("ship_by_date").asLong(0);
        return Optional.of(new PedidoImportado(
                codigo,
                status.get(),
                Instant.ofEpochSecond(pedido.path("create_time").asLong()),
                prazo > 0 ? Instant.ofEpochSecond(prazo) : null,
                cupomDoVendedor,
                taxas,
                itens));
    }

    private Optional<JsonNode> buscarRepasse(String codigo) {
        try {
            return Optional.of(chamarLoja(PATH_REPASSE, Map.of("order_sn", codigo)));
        } catch (ShopeeIntegracaoException e) {
            log.warn("Não consegui buscar as taxas do pedido {} ({}). Elas serão estimadas.", codigo, e.getMessage());
            return Optional.empty();
        }
    }

    private JsonNode chamarLoja(String path, Map<String, String> parametros) {
        try {
            return executarChamadaDaLoja(path, parametros);
        } catch (TokenRecusadoException primeiraRecusa) {
            renovarToken();
            try {
                return executarChamadaDaLoja(path, parametros);
            } catch (TokenRecusadoException segundaRecusa) {
                throw new ShopeeIntegracaoException("A Shopee recusou o token mesmo depois de renovado", segundaRecusa);
            }
        }
    }

    private JsonNode executarChamadaDaLoja(String path, Map<String, String> parametros) {
        long timestamp = relogio.instant().getEpochSecond();
        String token = accessToken;
        String assinatura = AssinaturaShopee.assinar(partnerKey,
                AssinaturaShopee.textoBaseDaLoja(partnerId, path, timestamp, token, shopId));
        JsonNode corpo;
        try {
            corpo = http.get()
                    .uri(uri -> {
                        uri.path(path)
                                .queryParam("partner_id", partnerId)
                                .queryParam("timestamp", timestamp)
                                .queryParam("access_token", token)
                                .queryParam("shop_id", shopId)
                                .queryParam("sign", assinatura);
                        parametros.forEach(uri::queryParam);
                        return uri.build();
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (requisicao, resposta) -> {
                        // A Shopee explica o erro no corpo JSON; ele é tratado logo abaixo.
                    })
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new ShopeeIntegracaoException("Falha ao chamar a Shopee em " + path, e);
        }
        verificarErro(path, corpo, true);
        return corpo.path("response");
    }

    private synchronized void renovarToken() {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ShopeeIntegracaoException("O access_token foi recusado e não há refresh_token configurado");
        }
        long timestamp = relogio.instant().getEpochSecond();
        String assinatura = AssinaturaShopee.assinar(partnerKey,
                AssinaturaShopee.textoBasePublico(partnerId, PATH_RENOVAR_TOKEN, timestamp));
        JsonNode corpo;
        try {
            corpo = http.post()
                    .uri(uri -> uri.path(PATH_RENOVAR_TOKEN)
                            .queryParam("partner_id", partnerId)
                            .queryParam("timestamp", timestamp)
                            .queryParam("sign", assinatura)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("refresh_token", refreshToken, "partner_id", partnerId, "shop_id", shopId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (requisicao, resposta) -> {
                    })
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new ShopeeIntegracaoException("Falha ao renovar o token da Shopee", e);
        }
        verificarErro(PATH_RENOVAR_TOKEN, corpo, false);
        accessToken = corpo.path("access_token").asText();
        refreshToken = corpo.path("refresh_token").asText(refreshToken);
        log.info("Token de acesso da Shopee renovado");
    }

    private static void verificarErro(String path, JsonNode corpo, boolean podeSerToken) {
        if (corpo == null) {
            throw new ShopeeIntegracaoException("A Shopee respondeu sem corpo em " + path);
        }
        String erro = corpo.path("error").asText("");
        if (erro.isBlank()) {
            return;
        }
        String mensagem = corpo.path("message").asText("");
        if (podeSerToken && (erro.contains("access_token") || erro.contains("acceess_token") || erro.equals("error_auth"))) {
            throw new TokenRecusadoException(erro + ": " + mensagem);
        }
        throw new ShopeeIntegracaoException("A Shopee recusou a chamada " + path + " (" + erro + "): " + mensagem);
    }

    private static BigDecimal decimal(JsonNode no, String campo) {
        JsonNode valor = no.path(campo);
        if (valor.isNumber()) {
            return valor.decimalValue();
        }
        if (valor.isTextual() && !valor.asText().isBlank()) {
            try {
                return new BigDecimal(valor.asText().trim());
            } catch (NumberFormatException e) {
                return Moeda.ZERO;
            }
        }
        return Moeda.ZERO;
    }

    static class TokenRecusadoException extends RuntimeException {
        TokenRecusadoException(String mensagem) {
            super(mensagem);
        }
    }
}
