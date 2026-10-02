package io.github.thibrown1.margem.adaptador.saida.shopee;

import io.github.thibrown1.margem.dominio.pedido.StatusPedido;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

/** Traduz o status da Shopee (código da API ou texto da planilha) para o status do sistema. */
public final class StatusShopee {

    private StatusShopee() {
    }

    /** Códigos da Open Platform v2 (campo order_status). */
    public static Optional<StatusPedido> doCodigoDaApi(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(switch (codigo.trim().toUpperCase(Locale.ROOT)) {
            case "UNPAID" -> StatusPedido.NAO_PAGO;
            case "READY_TO_SHIP", "PROCESSED", "RETRY_SHIP", "IN_CANCEL" -> StatusPedido.AGUARDANDO_ENVIO;
            case "SHIPPED", "TO_CONFIRM_RECEIVE" -> StatusPedido.ENVIADO;
            case "COMPLETED" -> StatusPedido.CONCLUIDO;
            case "CANCELLED" -> StatusPedido.CANCELADO;
            case "TO_RETURN" -> StatusPedido.DEVOLVIDO;
            default -> null;
        });
    }

    /** Textos da planilha exportada da Central do Vendedor (português ou inglês). */
    public static Optional<StatusPedido> doTextoDaPlanilha(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String t = normalizar(texto);
        if (t.contains("cancel")) {
            return Optional.of(StatusPedido.CANCELADO);
        }
        if (t.contains("devol") || t.contains("reembols") || t.contains("return") || t.contains("refund")) {
            return Optional.of(StatusPedido.DEVOLVIDO);
        }
        if (t.contains("nao pago") || t.contains("aguardando pagamento") || t.contains("a pagar") || t.contains("unpaid")) {
            return Optional.of(StatusPedido.NAO_PAGO);
        }
        if (t.contains("conclu") || t.contains("complet") || t.contains("finaliz")) {
            return Optional.of(StatusPedido.CONCLUIDO);
        }
        if (t.contains("a enviar") || t.contains("para enviar") || t.contains("aguardando envio")
                || t.contains("pronto para envio") || t.contains("processad") || t.contains("ready to ship")
                || t.contains("to ship")) {
            return Optional.of(StatusPedido.AGUARDANDO_ENVIO);
        }
        if (t.contains("enviado") || t.contains("a caminho") || t.contains("em transito") || t.contains("entregue")
                || t.contains("shipped") || t.contains("delivered")) {
            return Optional.of(StatusPedido.ENVIADO);
        }
        return doCodigoDaApi(texto);
    }

    static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
