package io.github.thibrown1.margem.configuracao;

import io.github.thibrown1.margem.adaptador.saida.notificacao.LogNotificador;
import io.github.thibrown1.margem.adaptador.saida.notificacao.TelegramNotificador;
import io.github.thibrown1.margem.adaptador.saida.shopee.ShopeeApiGateway;
import io.github.thibrown1.margem.adaptador.saida.shopee.ShopeeSimuladaGateway;
import io.github.thibrown1.margem.aplicacao.porta.Notificador;
import io.github.thibrown1.margem.aplicacao.porta.ShopeeGateway;
import io.github.thibrown1.margem.dominio.taxa.TabelaTaxasShopee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/** Escolhe, pela configuração, qual adaptador atende cada porta de saída. */
@Configuration
public class IntegracoesConfig {

    private static final Logger log = LoggerFactory.getLogger(IntegracoesConfig.class);

    @Bean
    public ShopeeGateway shopeeGateway(MargemProperties propriedades, TabelaTaxasShopee tabela, Clock relogio,
                                       RestClient.Builder construtor) {
        MargemProperties.Shopee shopee = propriedades.shopee();
        if (shopee.modo() == MargemProperties.Shopee.Modo.SIMULADA) {
            log.info("Usando a Shopee simulada (margem.shopee.modo=simulada)");
            return new ShopeeSimuladaGateway(tabela, relogio);
        }

        MargemProperties.Shopee.Api api = shopee.api();
        List<String> faltando = new ArrayList<>();
        if (api.partnerId() == null) {
            faltando.add("SHOPEE_PARTNER_ID");
        }
        if (api.partnerKey() == null || api.partnerKey().isBlank()) {
            faltando.add("SHOPEE_PARTNER_KEY");
        }
        if (api.shopId() == null) {
            faltando.add("SHOPEE_SHOP_ID");
        }
        if (api.accessToken() == null || api.accessToken().isBlank()) {
            faltando.add("SHOPEE_ACCESS_TOKEN");
        }
        if (!faltando.isEmpty()) {
            throw new IllegalStateException("margem.shopee.modo=api, mas faltam as configurações: " + String.join(", ", faltando));
        }
        log.info("Usando a API da Shopee em {}", api.host());
        return new ShopeeApiGateway(construtor.clone().baseUrl(api.host()).build(), api.partnerId(), api.partnerKey(),
                api.shopId(), api.accessToken(), api.refreshToken(), relogio);
    }

    @Bean
    public Notificador notificador(MargemProperties propriedades, RestClient.Builder construtor) {
        MargemProperties.Telegram telegram = propriedades.telegram();
        if (!telegram.configurado()) {
            log.info("Telegram não configurado: os alertas vão aparecer no log");
            return new LogNotificador();
        }
        return new TelegramNotificador(construtor.clone().baseUrl(telegram.urlBase()).build(),
                telegram.botToken(), telegram.chatId());
    }
}
