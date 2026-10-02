package io.github.thibrown1.margem.adaptador.entrada.web;

import io.github.thibrown1.margem.adaptador.entrada.planilha.LeitorPlanilhaShopee;
import io.github.thibrown1.margem.adaptador.entrada.planilha.LeituraPlanilha;
import io.github.thibrown1.margem.adaptador.entrada.planilha.PlanilhaInvalidaException;
import io.github.thibrown1.margem.aplicacao.Pagina;
import io.github.thibrown1.margem.aplicacao.pedido.ConsultaPedidosService;
import io.github.thibrown1.margem.aplicacao.pedido.ImportacaoPedidosService;
import io.github.thibrown1.margem.aplicacao.pedido.PedidoAEnviar;
import io.github.thibrown1.margem.aplicacao.pedido.PedidoResumo;
import io.github.thibrown1.margem.aplicacao.pedido.ResultadoImportacao;
import io.github.thibrown1.margem.aplicacao.pedido.SincronizacaoShopeeService;
import io.github.thibrown1.margem.dominio.lucro.LucroPedido;
import io.github.thibrown1.margem.dominio.pedido.OrigemPedido;
import io.github.thibrown1.margem.dominio.pedido.StatusPedido;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/pedidos")
@Tag(name = "4. Pedidos", description = "Importação da planilha da Shopee, sincronização pela API e consulta de pedidos.")
public class PedidoController {

    private final LeitorPlanilhaShopee leitor;
    private final ImportacaoPedidosService importacao;
    private final SincronizacaoShopeeService sincronizacao;
    private final ConsultaPedidosService consulta;

    public PedidoController(LeitorPlanilhaShopee leitor, ImportacaoPedidosService importacao,
                            SincronizacaoShopeeService sincronizacao, ConsultaPedidosService consulta) {
        this.leitor = leitor;
        this.importacao = importacao;
        this.sincronizacao = sincronizacao;
        this.consulta = consulta;
    }

    @PostMapping(path = "/importacoes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importa a planilha de pedidos exportada da Central do Vendedor (.xlsx)",
            description = "Pode importar a mesma planilha mais de uma vez: pedidos existentes são atualizados, não duplicados."
                    + " Colunas com dados do comprador (nome, endereço, telefone) são ignoradas.")
    public ResultadoImportacao importarPlanilha(@RequestPart("arquivo") MultipartFile arquivo) {
        String nome = arquivo.getOriginalFilename() == null ? "" : arquivo.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (arquivo.isEmpty() || !nome.endsWith(".xlsx")) {
            throw new PlanilhaInvalidaException("Envie o arquivo .xlsx exportado em Central do Vendedor > Meus Pedidos > Exportar.");
        }
        LeituraPlanilha leitura;
        try (InputStream conteudo = arquivo.getInputStream()) {
            leitura = leitor.ler(conteudo);
        } catch (IOException e) {
            throw new PlanilhaInvalidaException("Não consegui ler o arquivo enviado.");
        }
        return importacao.importar(leitura.pedidos(), OrigemPedido.PLANILHA).comErrosDeLeitura(leitura.erros());
    }

    @PostMapping("/sincronizacoes")
    @Operation(summary = "Busca os pedidos recentes direto na Shopee",
            description = "Usa a API oficial quando configurada, ou a Shopee simulada na demonstração.")
    public ResultadoImportacao sincronizar(
            @Parameter(description = "Quantos dias para trás buscar") @RequestParam(defaultValue = "7") @Min(1) @Max(90) int dias) {
        return sincronizacao.sincronizar(dias);
    }

    @GetMapping
    @Operation(summary = "Lista pedidos por período e status", description = "Sem datas, mostra os últimos 30 dias. DONO e EXPEDICAO.")
    public Pagina<PedidoResumo> listar(
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
        return consulta.listar(status, inicio, fim, pagina, tamanho);
    }

    @GetMapping("/a-enviar")
    @Operation(summary = "Fila de expedição: pedidos pagos ainda não enviados, do prazo mais apertado para o mais folgado",
            description = "DONO e EXPEDICAO.")
    public List<PedidoAEnviar> aEnviar() {
        return consulta.aEnviar();
    }

    @GetMapping("/{codigo}/lucro")
    @Operation(summary = "Conta aberta do pedido: quanto o cliente pagou, quanto a Shopee cobrou e quanto sobrou")
    public LucroPedido lucro(@PathVariable String codigo) {
        return consulta.lucroDoPedido(codigo);
    }
}
