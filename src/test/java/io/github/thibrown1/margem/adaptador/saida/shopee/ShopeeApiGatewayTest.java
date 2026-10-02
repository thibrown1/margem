package io.github.thibrown1.margem.adaptador.saida.shopee;

import io.github.thibrown1.margem.aplicacao.pedido.PedidoImportado;
import io.github.thibrown1.margem.dominio.pedido.StatusPedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ShopeeApiGatewayTest {

    private static final String HOST = "https://shopee.teste";
    private static final Instant AGORA = Instant.ofEpochSecond(1_790_000_000L);
    /** HMAC-SHA256 de "2001887/api/v2/order/get_order_list1790000000token-atual55555" com a chave "chave-parceiro", calculado fora do Java. */
    private static final String ASSINATURA_ESPERADA = "40da28cd12eeea70372518fe7649e7f27ede8d08cc6daead2122f00da24b7fcb";

    private MockRestServiceServer shopee;
    private ShopeeApiGateway gateway;

    @BeforeEach
    void configurar() {
        RestClient.Builder construtor = RestClient.builder().baseUrl(HOST);
        shopee = MockRestServiceServer.bindTo(construtor).build();
        gateway = new ShopeeApiGateway(construtor.build(), 2_001_887L, "chave-parceiro", 55_555L, "token-atual",
                "refresh-atual", Clock.fixed(AGORA, ZoneId.of("America/Sao_Paulo")));
    }

    @Test
    void assinaAChamadaComoAShopeeExige() {
        assertThat(AssinaturaShopee.assinar("chave-parceiro",
                AssinaturaShopee.textoBaseDaLoja(2_001_887L, "/api/v2/order/get_order_list", 1_790_000_000L, "token-atual", 55_555L)))
                .isEqualTo(ASSINATURA_ESPERADA);
    }

    @Test
    void buscaListaPaginadaDetalhesETaxasDosPedidos() {
        shopee.expect(requestTo(startsWith(HOST + ShopeeApiGateway.PATH_LISTA_PEDIDOS)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("partner_id", "2001887"))
                .andExpect(queryParam("shop_id", "55555"))
                .andExpect(queryParam("timestamp", "1790000000"))
                .andExpect(queryParam("sign", ASSINATURA_ESPERADA))
                .andExpect(queryParam("cursor", ""))
                .andRespond(json("""
                        {"error":"","message":"","response":{"more":true,"next_cursor":"pagina2","order_list":[{"order_sn":"A1"}]}}"""));
        shopee.expect(requestTo(startsWith(HOST + ShopeeApiGateway.PATH_LISTA_PEDIDOS)))
                .andExpect(queryParam("cursor", "pagina2"))
                .andRespond(json("""
                        {"error":"","response":{"more":false,"next_cursor":"","order_list":[{"order_sn":"A2"}]}}"""));
        shopee.expect(requestTo(startsWith(HOST + ShopeeApiGateway.PATH_DETALHES_PEDIDOS)))
                .andExpect(queryParam("order_sn_list", "A1,A2"))
                .andRespond(json("""
                        {"error":"","response":{"order_list":[
                          {"order_sn":"A1","order_status":"COMPLETED","create_time":1789900000,"ship_by_date":1790072800,
                           "item_list":[{"item_name":"Garrafa Térmica","model_name":"Preta","item_sku":"GARRAFA",
                                         "model_sku":"GARRAFA-PRETA","model_quantity_purchased":2,
                                         "model_original_price":69.9,"model_discounted_price":59.9}]},
                          {"order_sn":"A2","order_status":"CANCELLED","create_time":1789950000,
                           "item_list":[{"item_name":"Suporte","model_name":"","item_sku":"","model_sku":"",
                                         "model_quantity_purchased":1,"model_original_price":19.9,"model_discounted_price":0}]}
                        ]}}"""));
        shopee.expect(requestTo(startsWith(HOST + ShopeeApiGateway.PATH_REPASSE)))
                .andExpect(queryParam("order_sn", "A1"))
                .andRespond(json("""
                        {"error":"","response":{"order_sn":"A1","order_income":{
                          "commission_fee":23.96,"service_fee":8.0,"seller_transaction_fee":0,"voucher_from_seller":5.0}}}"""));

        List<PedidoImportado> pedidos = gateway.buscarPedidos(AGORA.minus(Duration.ofDays(3)), AGORA);

        shopee.verify();
        assertThat(pedidos).hasSize(2);
        PedidoImportado concluido = pedidos.getFirst();
        assertThat(concluido.status()).isEqualTo(StatusPedido.CONCLUIDO);
        assertThat(concluido.prazoEnvio()).isEqualTo(Instant.ofEpochSecond(1_790_072_800L));
        assertThat(concluido.descontoVendedor()).isEqualByComparingTo("5.00");
        assertThat(concluido.taxas().comissao()).isEqualByComparingTo("23.96");
        assertThat(concluido.taxas().servico()).isEqualByComparingTo("8.00");
        assertThat(concluido.itens()).singleElement().satisfies(item -> {
            assertThat(item.sku()).isEqualTo("GARRAFA-PRETA");
            assertThat(item.descricao()).isEqualTo("Garrafa Térmica - Preta");
            assertThat(item.quantidade()).isEqualTo(2);
            assertThat(item.precoUnitario()).isEqualByComparingTo("59.90");
        });

        PedidoImportado cancelado = pedidos.get(1);
        assertThat(cancelado.status()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(cancelado.taxas()).as("pedido cancelado não busca repasse").isNull();
        assertThat(cancelado.itens().getFirst().sku()).as("sem SKU no anúncio, usa o nome").isEqualTo("SUPORTE");
        assertThat(cancelado.itens().getFirst().precoUnitario()).isEqualByComparingTo("19.90");
    }

    @Test
    void divideOPeriodoEmJanelasDe15Dias() {
        shopee.expect(ExpectedCount.times(3), requestTo(startsWith(HOST + ShopeeApiGateway.PATH_LISTA_PEDIDOS)))
                .andRespond(json("""
                        {"error":"","response":{"more":false,"order_list":[]}}"""));

        List<PedidoImportado> pedidos = gateway.buscarPedidos(AGORA.minus(Duration.ofDays(40)), AGORA);

        shopee.verify();
        assertThat(pedidos).isEmpty();
    }

    @Test
    void renovaOTokenExpiradoETentaDeNovo() {
        shopee.expect(requestTo(startsWith(HOST + ShopeeApiGateway.PATH_LISTA_PEDIDOS)))
                .andExpect(queryParam("access_token", "token-atual"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON).body("""
                        {"error":"invalid_acceess_token","message":"Invalid access_token."}"""));
        shopee.expect(requestTo(startsWith(HOST + ShopeeApiGateway.PATH_RENOVAR_TOKEN)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(queryParam("sign", "26ae7dfd669cf9ec55455c201630a4de9e5bf79890a793eadc4caf9aa808d8f0"))
                .andExpect(jsonPath("$.refresh_token").value("refresh-atual"))
                .andExpect(jsonPath("$.shop_id").value(55555))
                .andRespond(json("""
                        {"error":"","access_token":"token-novo","refresh_token":"refresh-novo","expire_in":14400}"""));
        shopee.expect(requestTo(startsWith(HOST + ShopeeApiGateway.PATH_LISTA_PEDIDOS)))
                .andExpect(queryParam("access_token", "token-novo"))
                .andRespond(json("""
                        {"error":"","response":{"more":false,"order_list":[]}}"""));

        gateway.buscarPedidos(AGORA.minus(Duration.ofDays(1)), AGORA);

        shopee.verify();
    }

    @Test
    void outrosErrosDaShopeeViramExcecaoComMensagemClara() {
        shopee.expect(requestTo(startsWith(HOST + ShopeeApiGateway.PATH_LISTA_PEDIDOS)))
                .andRespond(json("""
                        {"error":"error_param","message":"time range is too long"}"""));

        assertThatThrownBy(() -> gateway.buscarPedidos(AGORA.minus(Duration.ofDays(1)), AGORA))
                .isInstanceOf(ShopeeIntegracaoException.class)
                .hasMessageContaining("error_param")
                .hasMessageContaining("time range is too long");
    }

    private static org.springframework.test.web.client.ResponseCreator json(String corpo) {
        return withSuccess(corpo, MediaType.APPLICATION_JSON);
    }
}
