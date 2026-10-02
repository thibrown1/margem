package io.github.thibrown1.margem.aplicacao.pedido;

import io.github.thibrown1.margem.aplicacao.Pagina;
import io.github.thibrown1.margem.aplicacao.Periodo;
import io.github.thibrown1.margem.aplicacao.RecursoNaoEncontradoException;
import io.github.thibrown1.margem.aplicacao.porta.PedidoRepositorio;
import io.github.thibrown1.margem.dominio.RegraDeNegocioException;
import io.github.thibrown1.margem.dominio.lucro.CalculadoraLucro;
import io.github.thibrown1.margem.dominio.lucro.LucroPedido;
import io.github.thibrown1.margem.dominio.pedido.Pedido;
import io.github.thibrown1.margem.dominio.pedido.StatusPedido;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ConsultaPedidosService {

    public static final int TAMANHO_MAXIMO_DA_PAGINA = 100;

    private final PedidoRepositorio pedidos;
    private final CalculadoraLucro calculadora;
    private final Clock relogio;
    private final ZoneId zona;

    public ConsultaPedidosService(PedidoRepositorio pedidos, CalculadoraLucro calculadora, Clock relogio) {
        this.pedidos = pedidos;
        this.calculadora = calculadora;
        this.relogio = relogio;
        this.zona = relogio.getZone();
    }

    public Pagina<PedidoResumo> listar(StatusPedido status, LocalDate inicio, LocalDate fim, int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > TAMANHO_MAXIMO_DA_PAGINA) {
            throw new RegraDeNegocioException("Página deve ser >= 0 e tamanho entre 1 e " + TAMANHO_MAXIMO_DA_PAGINA);
        }
        Periodo periodo = Periodo.de(inicio, fim, relogio);
        return pedidos.listar(status, periodo.inicioEm(zona), periodo.fimEm(zona), pagina, tamanho)
                .mapear(this::resumir);
    }

    public List<PedidoAEnviar> aEnviar() {
        Instant agora = relogio.instant();
        return pedidos.buscarAguardandoEnvio().stream()
                .map(pedido -> paraExpedicao(pedido, agora))
                .toList();
    }

    public LucroPedido lucroDoPedido(String codigo) {
        Pedido pedido = pedidos.buscarPorCodigo(codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido " + codigo + " não encontrado"));
        return calculadora.calcular(pedido);
    }

    private PedidoResumo resumir(Pedido pedido) {
        return new PedidoResumo(pedido.getCodigo(), pedido.getStatus(), pedido.getOrigem(), local(pedido.getCriadoEm()),
                local(pedido.getPrazoEnvio()), pedido.quantidadeDeItens(), pedido.valorBruto());
    }

    private PedidoAEnviar paraExpedicao(Pedido pedido, Instant agora) {
        Long horasRestantes = pedido.getPrazoEnvio() == null
                ? null
                : Duration.between(agora, pedido.getPrazoEnvio()).toHours();
        boolean atrasado = pedido.getPrazoEnvio() != null && pedido.getPrazoEnvio().isBefore(agora);
        List<String> itens = pedido.getItens().stream()
                .map(item -> item.getQuantidade() + "x " + item.getDescricao() + " (" + item.getSku() + ")")
                .toList();
        return new PedidoAEnviar(pedido.getCodigo(), local(pedido.getCriadoEm()), local(pedido.getPrazoEnvio()),
                horasRestantes, atrasado, itens);
    }

    private OffsetDateTime local(Instant instante) {
        return instante == null ? null : instante.atZone(zona).toOffsetDateTime();
    }
}
