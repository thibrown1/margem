package io.github.thibrown1.margem.adaptador.saida.notificacao;

import io.github.thibrown1.margem.aplicacao.porta.Notificador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Usado quando o Telegram não está configurado: os alertas aparecem no log da aplicação. */
public class LogNotificador implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(LogNotificador.class);

    @Override
    public void enviar(String mensagem) {
        log.info("Alerta (Telegram não configurado):\n{}", mensagem);
    }

    @Override
    public String canal() {
        return "log";
    }
}
