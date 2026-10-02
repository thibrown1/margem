package io.github.thibrown1.margem.aplicacao.alerta;

import io.github.thibrown1.margem.aplicacao.porta.AlertaRepositorio;
import io.github.thibrown1.margem.aplicacao.porta.Notificador;
import io.github.thibrown1.margem.aplicacao.porta.PedidoRepositorio;
import io.github.thibrown1.margem.aplicacao.porta.ProdutoRepositorio;
import io.github.thibrown1.margem.dominio.alerta.Alerta;
import io.github.thibrown1.margem.dominio.alerta.TipoAlerta;
import io.github.thibrown1.margem.dominio.lucro.CalculadoraLucro;
import io.github.thibrown1.margem.dominio.lucro.ParametrosLoja;
import io.github.thibrown1.margem.dominio.pedido.ItemPedido;
import io.github.thibrown1.margem.dominio.pedido.OrigemPedido;
import io.github.thibrown1.margem.dominio.pedido.Pedido;
import io.github.thibrown1.margem.dominio.pedido.StatusPedido;
import io.github.thibrown1.margem.dominio.pedido.TaxasShopee;
import io.github.thibrown1.margem.dominio.produto.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-09-29T15:00:00Z");

    @Mock
    private PedidoRepositorio pedidos;
    @Mock
    private ProdutoRepositorio produtos;
    @Mock
    private AlertaRepositorio historico;
    @Mock
    private Notificador notificador;

    private AlertaService servico;

    @BeforeEach
    void configurar() {
        servico = new AlertaService(pedidos, produtos, historico, notificador,
                new CalculadoraLucro(new ParametrosLoja(new BigDecimal("1.50"), new BigDecimal("4"))),
                new ParametrosAlertas(Duration.ofHours(24), Duration.ofHours(24), Duration.ofDays(7)),
                Clock.fixed(AGORA, ZoneId.of("America/Sao_Paulo")));
        lenient().when(notificador.canal()).thenReturn("teste");
    }

    @Test
    void juntaTodosOsAlertasNumaMensagemSo() {
        when(pedidos.buscarAguardandoEnvio()).thenReturn(List.of(
                pedidoAEnviar("ATRASADO", AGORA.minus(Duration.ofHours(3))),
                pedidoAEnviar("VENCE-HOJE", AGORA.plus(Duration.ofHours(5))),
                pedidoAEnviar("TRANQUILO", AGORA.plus(Duration.ofHours(40)))));
        when(produtos.listarTodos()).thenReturn(List.of(
                new Produto(1L, "LUMINARIA-LED", "Luminária", BigDecimal.TEN, 2, 5),
                new Produto(2L, "GARRAFA", "Garrafa", BigDecimal.TEN, 50, 5)));
        when(pedidos.buscarCriadosEntre(any(), any())).thenReturn(List.of(pedidoNoPrejuizo()));

        ResultadoAlertas resultado = servico.verificarEEnviar();

        assertThat(resultado.alertasEnviados()).extracting(Alerta::referencia)
                .containsExactly("ATRASADO", "VENCE-HOJE", "LUMINARIA-LED", "PREJUIZO");
        ArgumentCaptor<String> mensagem = ArgumentCaptor.forClass(String.class);
        verify(notificador).enviar(mensagem.capture());
        assertThat(mensagem.getValue())
                .contains("ATRASADO há 3h")
                .contains("faltam 5h")
                .contains("LUMINARIA-LED (Luminária): 2 un., mínimo 5")
                .contains("prejuízo de R$ 4,28")
                .doesNotContain("TRANQUILO");
        verify(historico).registrarEnvio(resultado.alertasEnviados(), AGORA);
    }

    @Test
    void naoRepeteAlertaJaEnviado() {
        when(pedidos.buscarAguardandoEnvio()).thenReturn(List.of(pedidoAEnviar("P1", AGORA.plus(Duration.ofHours(2)))));
        when(produtos.listarTodos()).thenReturn(List.of());
        when(pedidos.buscarCriadosEntre(any(), any())).thenReturn(List.of());
        when(historico.foiEnviadoDesde(eq(TipoAlerta.PRAZO_ENVIO), eq("P1"), any())).thenReturn(true);

        ResultadoAlertas resultado = servico.verificarEEnviar();

        assertThat(resultado.alertasEnviados()).isEmpty();
        assertThat(resultado.alertasRepetidos()).isEqualTo(1);
        verify(notificador, never()).enviar(anyString());
    }

    @Test
    void seOEnvioFalharNadaEhMarcadoComoEnviado() {
        when(pedidos.buscarAguardandoEnvio()).thenReturn(List.of(pedidoAEnviar("P1", AGORA.plus(Duration.ofHours(2)))));
        when(produtos.listarTodos()).thenReturn(List.of());
        when(pedidos.buscarCriadosEntre(any(), any())).thenReturn(List.of());
        doThrow(new IllegalStateException("Telegram fora do ar")).when(notificador).enviar(anyString());

        assertThatThrownBy(() -> servico.verificarEEnviar()).hasMessageContaining("Telegram");
        verify(historico, never()).registrarEnvio(anyList(), any());
    }

    private static Pedido pedidoAEnviar(String codigo, Instant prazo) {
        return new Pedido(1L, codigo, StatusPedido.AGUARDANDO_ENVIO, OrigemPedido.API_SHOPEE, prazo.minus(Duration.ofDays(2)),
                prazo, BigDecimal.ZERO, TaxasShopee.NENHUMA, false,
                List.of(ItemPedido.novo("GARRAFA", "Garrafa", 1, BigDecimal.TEN, BigDecimal.ONE)));
    }

    private static Pedido pedidoNoPrejuizo() {
        return new Pedido(2L, "PREJUIZO", StatusPedido.CONCLUIDO, OrigemPedido.API_SHOPEE, AGORA.minus(Duration.ofDays(2)),
                null, BigDecimal.ZERO, new TaxasShopee(new BigDecimal("3.98"), new BigDecimal("4.00"), null), false,
                List.of(ItemPedido.novo("SUPORTE-CELULAR", "Suporte", 1, new BigDecimal("19.90"), new BigDecimal("13.90"))));
    }
}
