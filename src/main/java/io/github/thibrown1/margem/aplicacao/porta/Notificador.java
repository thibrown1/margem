package io.github.thibrown1.margem.aplicacao.porta;

/** Porta de saída para avisar o dono da loja (Telegram em produção, log em desenvolvimento). */
public interface Notificador {

    void enviar(String mensagem);

    String canal();
}
