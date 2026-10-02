package io.github.thibrown1.margem.aplicacao.alerta;

import io.github.thibrown1.margem.aplicacao.porta.AlertaRepositorio;
import io.github.thibrown1.margem.aplicacao.porta.Notificador;
import io.github.thibrown1.margem.aplicacao.porta.PedidoRepositorio;
import io.github.thibrown1.margem.aplicacao.porta.ProdutoRepositorio;
import io.github.thibrown1.margem.dominio.Moeda;
import io.github.thibrown1.margem.dominio.alerta.Alerta;
import io.github.thibrown1.margem.dominio.alerta.MensagemAlertas;
import io.github.thibrown1.margem.dominio.alerta.TipoAlerta;
import io.github.thibrown1.margem.dominio.lucro.CalculadoraLucro;
import io.github.thibrown1.margem.dominio.lucro.LucroItem;
import io.github.thibrown1.margem.dominio.lucro.LucroPedido;
import io.github.thibrown1.margem.dominio.pedido.Pedido;
import io.github.thibrown1.margem.dominio.produto.Produto;
import io.github.thibrown1.margem.dominio.produto.SituacaoEstoque;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Verifica o que precisa de atenção e manda tudo numa mensagem só.
 * Um alerta já enviado não é repetido até passar o intervalo configurado.
 * Se o envio falhar, nada é marcado como enviado e a próxima verificação tenta de novo.
 */
@Service
public class AlertaService {

    private static final DateTimeFormatter DIA_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final PedidoRepositorio pedidos;
    private final ProdutoRepositorio produtos;
    private final AlertaRepositorio historico;
    private final Notificador notificador;
    private final CalculadoraLucro calculadora;
    private final ParametrosAlertas parametros;
    private final Clock relogio;

    public AlertaService(PedidoRepositorio pedidos, ProdutoRepositorio produtos, AlertaRepositorio historico,
                         Notificador notificador, CalculadoraLucro calculadora, ParametrosAlertas parametros, Clock relogio) {
        this.pedidos = pedidos;
        this.produtos = produtos;
        this.historico = historico;
        this.notificador = notificador;
        this.calculadora = calculadora;
        this.parametros = parametros;
        this.relogio = relogio;
    }

    @Transactional
    public ResultadoAlertas verificarEEnviar() {
        Instant agora = relogio.instant();
        List<Alerta> vigentes = new ArrayList<>();
        vigentes.addAll(prazosDeEnvio(agora));
        vigentes.addAll(estoqueBaixo());
        vigentes.addAll(pedidosNoPrejuizo(agora));

        Instant repetirSomenteDepoisDe = agora.minus(parametros.reenviarApos());
        List<Alerta> novos = vigentes.stream()
                .filter(alerta -> !historico.foiEnviadoDesde(alerta.tipo(), alerta.referencia(), repetirSomenteDepoisDe))
                .toList();

        if (!novos.isEmpty()) {
            notificador.enviar(MensagemAlertas.montar(novos, agora, relogio.getZone()));
            historico.registrarEnvio(novos, agora);
        }
        return new ResultadoAlertas(notificador.canal(), vigentes.size() - novos.size(), novos);
    }

    private List<Alerta> prazosDeEnvio(Instant agora) {
        Instant limite = agora.plus(parametros.antecedenciaDoPrazo());
        return pedidos.buscarAguardandoEnvio().stream()
                .filter(p -> p.getPrazoEnvio() != null && !p.getPrazoEnvio().isAfter(limite))
                .map(p -> new Alerta(TipoAlerta.PRAZO_ENVIO, p.getCodigo(), descreverPrazo(p, agora)))
                .toList();
    }

    private String descreverPrazo(Pedido pedido, Instant agora) {
        Duration restante = Duration.between(agora, pedido.getPrazoEnvio());
        String quando = DIA_HORA.format(pedido.getPrazoEnvio().atZone(relogio.getZone()));
        String situacao = restante.isNegative()
                ? "ATRASADO há " + horas(restante.negated())
                : "faltam " + horas(restante);
        return "Pedido " + pedido.getCodigo() + ": enviar até " + quando + " (" + situacao + ")";
    }

    private List<Alerta> estoqueBaixo() {
        return produtos.listarTodos().stream()
                .filter(p -> p.situacaoEstoque() != SituacaoEstoque.OK)
                .sorted(Comparator.comparing(Produto::situacaoEstoque).thenComparing(Produto::getSku))
                .map(p -> new Alerta(TipoAlerta.ESTOQUE_BAIXO, p.getSku(), p.getSku() + " (" + p.getNome() + "): "
                        + (p.getEstoqueAtual() <= 0 ? "sem estoque (" + p.getEstoqueAtual() + " un.)"
                        : p.getEstoqueAtual() + " un., mínimo " + p.getEstoqueMinimo())))
                .toList();
    }

    private List<Alerta> pedidosNoPrejuizo(Instant agora) {
        return pedidos.buscarCriadosEntre(agora.minus(parametros.janelaDePrejuizo()), agora).stream()
                .filter(Pedido::contaComoVenda)
                .filter(p -> !p.temCustoPendente())
                .map(calculadora::calcular)
                .filter(lucro -> lucro.lucro().signum() < 0)
                .map(lucro -> new Alerta(TipoAlerta.PEDIDO_NO_PREJUIZO, lucro.codigoPedido(),
                        "Pedido " + lucro.codigoPedido() + ": prejuízo de " + Moeda.formatar(lucro.lucro().negate())
                                + " (" + skus(lucro) + ")"))
                .toList();
    }

    private static String skus(LucroPedido lucro) {
        return lucro.itens().stream().map(LucroItem::sku).distinct().collect(Collectors.joining(", "));
    }

    private static String horas(Duration duracao) {
        long horas = duracao.toHours();
        return horas >= 1 ? horas + "h" : duracao.toMinutes() + "min";
    }
}
