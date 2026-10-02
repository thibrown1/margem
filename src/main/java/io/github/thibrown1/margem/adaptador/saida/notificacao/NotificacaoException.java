package io.github.thibrown1.margem.adaptador.saida.notificacao;

/** Falha ao enviar a notificação. Vira HTTP 502 na API. */
public class NotificacaoException extends RuntimeException {

    public NotificacaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
