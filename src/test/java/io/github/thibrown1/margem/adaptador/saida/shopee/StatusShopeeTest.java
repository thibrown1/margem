package io.github.thibrown1.margem.adaptador.saida.shopee;

import io.github.thibrown1.margem.dominio.pedido.StatusPedido;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class StatusShopeeTest {

    @ParameterizedTest
    @CsvSource({
            "A Enviar, AGUARDANDO_ENVIO",
            "Pronto para envio, AGUARDANDO_ENVIO",
            "Enviado, ENVIADO",
            "Concluído, CONCLUIDO",
            "Cancelado, CANCELADO",
            "Devolução/Reembolso, DEVOLVIDO",
            "Não pago, NAO_PAGO",
            "To ship, AGUARDANDO_ENVIO",
            "Completed, CONCLUIDO"})
    void entendeOStatusEscritoNaPlanilha(String texto, StatusPedido esperado) {
        assertThat(StatusShopee.doTextoDaPlanilha(texto)).contains(esperado);
    }

    @ParameterizedTest
    @CsvSource({
            "UNPAID, NAO_PAGO",
            "READY_TO_SHIP, AGUARDANDO_ENVIO",
            "PROCESSED, AGUARDANDO_ENVIO",
            "SHIPPED, ENVIADO",
            "TO_CONFIRM_RECEIVE, ENVIADO",
            "COMPLETED, CONCLUIDO",
            "CANCELLED, CANCELADO",
            "TO_RETURN, DEVOLVIDO"})
    void entendeOsCodigosDaApi(String codigo, StatusPedido esperado) {
        assertThat(StatusShopee.doCodigoDaApi(codigo)).contains(esperado);
    }

    @Test
    void statusDesconhecidoNaoEhAdivinhado() {
        assertThat(StatusShopee.doTextoDaPlanilha("Qualquer coisa")).isEmpty();
        assertThat(StatusShopee.doCodigoDaApi("NOVO_STATUS")).isEmpty();
    }
}
