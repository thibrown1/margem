package io.github.thibrown1.margem.adaptador.saida.notificacao;

import io.github.thibrown1.margem.aplicacao.porta.Notificador;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Envia mensagens por um bot do Telegram (método sendMessage da Bot API).
 * O Telegram aceita até 4096 caracteres por mensagem, então textos maiores são divididos por linha.
 */
public class TelegramNotificador implements Notificador {

    static final int TAMANHO_MAXIMO = 4000;

    private final RestClient http;
    private final String token;
    private final String chatId;

    public TelegramNotificador(RestClient http, String token, String chatId) {
        this.http = http;
        this.token = token;
        this.chatId = chatId;
    }

    @Override
    public void enviar(String mensagem) {
        for (String parte : dividir(mensagem)) {
            try {
                http.post()
                        // pathSegment não codifica o ":" do token (a Bot API espera /bot<token>/sendMessage literal)
                        .uri(uri -> uri.pathSegment("bot" + token, "sendMessage").build())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("chat_id", chatId, "text", parte, "disable_web_page_preview", true))
                        .retrieve()
                        .toBodilessEntity();
            } catch (RestClientException e) {
                throw new NotificacaoException("Não consegui enviar a mensagem pelo Telegram", e);
            }
        }
    }

    @Override
    public String canal() {
        return "telegram";
    }

    static List<String> dividir(String mensagem) {
        List<String> partes = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String linha : mensagem.split("\n", -1)) {
            String pedaco = linha.length() > TAMANHO_MAXIMO ? linha.substring(0, TAMANHO_MAXIMO) : linha;
            if (atual.length() + pedaco.length() + 1 > TAMANHO_MAXIMO) {
                partes.add(atual.toString());
                atual.setLength(0);
            }
            if (!atual.isEmpty()) {
                atual.append('\n');
            }
            atual.append(pedaco);
        }
        if (!atual.isEmpty()) {
            partes.add(atual.toString());
        }
        return partes;
    }
}
