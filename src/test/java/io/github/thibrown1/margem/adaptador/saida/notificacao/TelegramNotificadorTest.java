package io.github.thibrown1.margem.adaptador.saida.notificacao;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TelegramNotificadorTest {

    @Test
    void enviaAMensagemPeloBot() {
        RestClient.Builder construtor = RestClient.builder().baseUrl("https://telegram.teste");
        MockRestServiceServer telegram = MockRestServiceServer.bindTo(construtor).build();
        telegram.expect(requestTo("https://telegram.teste/bot123:abc/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.chat_id").value("987"))
                .andExpect(jsonPath("$.text").value("Olá, loja!"))
                .andRespond(withSuccess("{\"ok\":true}", null));

        new TelegramNotificador(construtor.build(), "123:abc", "987").enviar("Olá, loja!");

        telegram.verify();
    }

    @Test
    void mensagemGrandeEhDivididaSemCortarLinhas() {
        String linha = "x".repeat(99);
        String mensagem = String.join("\n", java.util.Collections.nCopies(100, linha));

        List<String> partes = TelegramNotificador.dividir(mensagem);

        assertThat(partes).hasSize(3);
        assertThat(partes).allSatisfy(parte -> assertThat(parte.length()).isLessThanOrEqualTo(TelegramNotificador.TAMANHO_MAXIMO));
        assertThat(String.join("\n", partes)).isEqualTo(mensagem);

        RestClient.Builder construtor = RestClient.builder().baseUrl("https://telegram.teste");
        MockRestServiceServer telegram = MockRestServiceServer.bindTo(construtor).build();
        telegram.expect(ExpectedCount.times(3), requestTo("https://telegram.teste/botT/sendMessage"))
                .andRespond(withSuccess());
        new TelegramNotificador(construtor.build(), "T", "1").enviar(mensagem);
        telegram.verify();
    }

    @Test
    void falhaDoTelegramViraNotificacaoException() {
        RestClient.Builder construtor = RestClient.builder().baseUrl("https://telegram.teste");
        MockRestServiceServer telegram = MockRestServiceServer.bindTo(construtor).build();
        telegram.expect(requestTo("https://telegram.teste/botT/sendMessage")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        TelegramNotificador notificador = new TelegramNotificador(construtor.build(), "T", "1");

        assertThatThrownBy(() -> notificador.enviar("oi")).isInstanceOf(NotificacaoException.class);
    }
}
