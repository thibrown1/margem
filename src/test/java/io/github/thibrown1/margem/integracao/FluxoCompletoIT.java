package io.github.thibrown1.margem.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.thibrown1.margem.suporte.PlanilhaShopeeDeTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O dia a dia da loja, de ponta a ponta pela API: cadastrar produtos, importar a planilha da Shopee,
 * ver o lucro de cada pedido, os relatórios, a fila de envio e os alertas.
 * Os valores esperados foram calculados à mão (embalagem R$ 1,50 e imposto de 4%).
 */
class FluxoCompletoIT extends TesteDeIntegracao {

    private String token;

    @BeforeEach
    void cadastrarProdutos() throws Exception {
        token = tokenDoDono();
        cadastrar("GARRAFA-INOX-500", "Garrafa Térmica Inox 500ml", "22.40", 10, 3);
        cadastrar("SUPORTE-CELULAR", "Suporte de Celular Articulado", "13.90", 5, 2);
        cadastrar("ORGANIZADOR-GAVETA", "Organizador de Gaveta 8 Divisórias", "12.80", 20, 5);
    }

    @Test
    void doUploadDaPlanilhaAteOLucroDoMes() throws Exception {
        // 1. Importa a planilha exportada da Central do Vendedor
        JsonNode importacao = importar(planilhaDoMes());
        assertThat(importacao.get("pedidosNovos").asInt()).isEqualTo(5);
        assertThat(importacao.get("skusSemCadastro").toString()).isEqualTo("[\"CAPA-NOTEBOOK-15\"]");
        assertThat(importacao.get("erros")).isEmpty();

        // 2. Estoque baixou só pelas vendas (o pedido cancelado não conta)
        executar(get("/api/produtos/GARRAFA-INOX-500"), token).andExpect(jsonPath("$.estoqueAtual").value(8));
        executar(get("/api/produtos/ORGANIZADOR-GAVETA"), token).andExpect(jsonPath("$.estoqueAtual").value(18));

        // 3. Conta aberta de um pedido com dois produtos, cupom e taxas
        executar(get("/api/pedidos/2609220003/lucro"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorBruto").value(109.70))
                .andExpect(jsonPath("$.totalTaxasShopee").value(33.94))
                .andExpect(jsonPath("$.valorRecebido").value(70.76))
                .andExpect(jsonPath("$.lucro").value(17.07))
                .andExpect(jsonPath("$.margemPercentual").value(16.30))
                .andExpect(jsonPath("$.itens.length()").value(2));

        // 4. Resumo do mês, com aviso do produto sem custo
        JsonNode resumo = corpo(executar(get("/api/relatorios/resumo?inicio=2026-09-01&fim=2026-09-30"), token));
        assertThat(resumo.get("pedidos").asInt()).isEqualTo(4);
        assertThat(resumo.get("pedidosCanceladosOuDevolvidos").asInt()).isEqualTo(1);
        assertThat(resumo.get("faturamentoBruto").decimalValue()).isEqualByComparingTo("259.40");
        assertThat(resumo.get("lucroLiquido").decimalValue()).isEqualByComparingTo("78.03");
        assertThat(resumo.get("avisos").toString()).contains("CAPA-NOTEBOOK-15");

        // 5. Cadastrar o produto que faltava corrige o lucro das vendas antigas
        executar(post("/api/produtos").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                "sku", "capa-notebook-15", "nome", "Capa para Notebook 15,6", "custoUnitario", 28.50,
                "estoqueInicial", 12, "estoqueMinimo", 3))), token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itensDePedidosAntigosAtualizados").value(1));
        executar(get("/api/pedidos/2609280004/lucro"), token)
                .andExpect(jsonPath("$.custoPendente").value(false))
                .andExpect(jsonPath("$.lucro").value(19.12));
        JsonNode resumoCorrigido = corpo(executar(get("/api/relatorios/resumo?inicio=2026-09-01&fim=2026-09-30"), token));
        assertThat(resumoCorrigido.get("lucroLiquido").decimalValue()).isEqualByComparingTo("49.53");
        assertThat(resumoCorrigido.get("avisos")).isEmpty();

        // 6. O produto que mais vende no prejuízo aparece no relatório
        executar(get("/api/relatorios/prejuizo?inicio=2026-09-01&fim=2026-09-30"), token)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sku").value("SUPORTE-CELULAR"))
                .andExpect(jsonPath("$[0].lucro").value(-4.28));
    }

    @Test
    void reimportarNaoDuplicaECancelamentoDevolveEstoque() throws Exception {
        importar(planilhaDoMes());

        JsonNode mesmaPlanilha = importar(planilhaDoMes());
        assertThat(mesmaPlanilha.get("pedidosNovos").asInt()).isZero();
        assertThat(mesmaPlanilha.get("pedidosSemAlteracao").asInt()).isEqualTo(5);

        byte[] comCancelamento = PlanilhaShopeeDeTeste.padrao()
                .linha("2609220003", "Cancelado", "2026-09-22 15:00", null, "Garrafa Térmica Inox 500ml", "GARRAFA-INOX-500",
                        null, null, 59.90, 59.90, 1, 5, 0, 0, 0, null, null, null)
                .linha("2609220003", "Cancelado", "2026-09-22 15:00", null, "Organizador de Gaveta", "ORGANIZADOR-GAVETA",
                        null, null, 24.90, 24.90, 2, 5, 0, 0, 0, null, null, null)
                .bytes();
        JsonNode cancelamento = importar(comCancelamento);

        assertThat(cancelamento.get("pedidosAtualizados").asInt()).isEqualTo(1);
        executar(get("/api/produtos/GARRAFA-INOX-500"), token).andExpect(jsonPath("$.estoqueAtual").value(9));
        executar(get("/api/produtos/ORGANIZADOR-GAVETA"), token).andExpect(jsonPath("$.estoqueAtual").value(20));
        executar(get("/api/pedidos?inicio=2026-09-01&fim=2026-09-30&status=CANCELADO"), token)
                .andExpect(jsonPath("$.totalDeItens").value(2));
    }

    @Test
    void filaDeEnvioEAlertasSemRepetir() throws Exception {
        importar(planilhaDoMes());

        executar(get("/api/pedidos/a-enviar"), tokenDaExpedicao())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("2609280004"))
                .andExpect(jsonPath("$[0].horasRestantes").value(22))
                .andExpect(jsonPath("$[0].atrasado").value(false))
                .andExpect(jsonPath("$[0].itens[0]").value("1x Capa para Notebook 15,6 polegadas (CAPA-NOTEBOOK-15)"));

        executar(post("/api/alertas/verificacoes"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canal").value("log"))
                .andExpect(jsonPath("$.alertasEnviados.length()").value(2))
                .andExpect(jsonPath("$.alertasEnviados[0].tipo").value("PRAZO_ENVIO"))
                .andExpect(jsonPath("$.alertasEnviados[1].tipo").value("PEDIDO_NO_PREJUIZO"));

        executar(post("/api/alertas/verificacoes"), token)
                .andExpect(jsonPath("$.alertasEnviados.length()").value(0))
                .andExpect(jsonPath("$.alertasRepetidos").value(2));
    }

    @Test
    void listagemPaginadaDoMaisRecenteParaOMaisAntigo() throws Exception {
        importar(planilhaDoMes());

        executar(get("/api/pedidos?inicio=2026-09-01&fim=2026-09-30&tamanho=2"), token)
                .andExpect(jsonPath("$.totalDeItens").value(5))
                .andExpect(jsonPath("$.totalDePaginas").value(3))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].codigo").value("2609280004"))
                .andExpect(jsonPath("$.itens[0].criadoEm").value("2026-09-28T16:00:00-03:00"));
    }

    @Test
    void simuladorMostraQueVenderMaisCaroPodeDarMenosLucro() throws Exception {
        executar(post("/api/simulador/lucro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"precoVenda\": 84.90, \"custoProduto\": 31.50}"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lucro").value(20.61))
                .andExpect(jsonPath("$.alerta").value(org.hamcrest.Matchers.containsString("R$ 79,99")));

        executar(post("/api/simulador/preco-ideal").contentType(MediaType.APPLICATION_JSON)
                .content("{\"custoProduto\": 22.40, \"margemDesejada\": 20}"), token)
                .andExpect(jsonPath("$.precoVenda").value(49.82));

        executar(post("/api/simulador/preco-ideal").contentType(MediaType.APPLICATION_JSON)
                .content("{\"custoProduto\": 22.40, \"margemDesejada\": 90}"), token)
                .andExpect(status().isUnprocessableEntity());

        executar(get("/api/simulador/faixas-a-evitar"), token)
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[1].de").value(80.00))
                .andExpect(jsonPath("$[1].ate").value(88.77));

        executar(get("/api/simulador/taxas"), token)
                .andExpect(jsonPath("$.vigencia").value("2026-03-01"))
                .andExpect(jsonPath("$.faixas[1].ate").value(79.99));
    }

    @Test
    void errosVoltamClarosEEmPortugues() throws Exception {
        executar(post("/api/produtos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\": \"\", \"nome\": \"Sem SKU\", \"custoUnitario\": -1, \"estoqueInicial\": 1}"), token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.sku").exists())
                .andExpect(jsonPath("$.campos.custoUnitario").exists());

        cadastrarEsperando("GARRAFA-INOX-500", 409);

        executar(get("/api/pedidos/NAO-EXISTE/lucro"), token)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Pedido NAO-EXISTE não encontrado"));

        executar(multipart("/api/pedidos/importacoes")
                .file(new MockMultipartFile("arquivo", "pedidos.csv", "text/csv", "a,b".getBytes())), token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Planilha inválida"));
    }

    @Test
    void planilhaDeExemploDoRepositorioImportaSemErros() throws Exception {
        byte[] exemplo = Files.readAllBytes(Path.of("exemplos", "pedidos-shopee-exemplo.xlsx"));

        JsonNode resultado = importar(exemplo);

        assertThat(resultado.get("pedidosNovos").asInt()).isPositive();
        assertThat(resultado.get("erros")).isEmpty();
    }

    /**
     * Cinco pedidos de setembro: dois concluídos, um enviado com dois produtos e cupom,
     * um a enviar de um produto ainda não cadastrado e um cancelado.
     */
    private static byte[] planilhaDoMes() {
        return PlanilhaShopeeDeTeste.padrao()
                .linha("2609200001", "Concluído", "2026-09-20 10:00", "2026-09-22 10:00", "Garrafa Térmica Inox 500ml",
                        "GARRAFA-INOX-500", null, null, 69.90, 59.90, 1, 0, 11.98, 4.00, 0, "Cliente A", "(11) 90000-0001", "Rua A, 1")
                .linha("2609270002", "Concluído", "2026-09-27 11:00", "2026-09-29 11:00", "Suporte de Celular Articulado",
                        "SUPORTE-CELULAR", null, null, 19.90, 19.90, 1, 0, 3.98, 4.00, 0, "Cliente B", "(11) 90000-0002", "Rua B, 2")
                .linha("2609220003", "Enviado", "2026-09-22 15:00", "2026-09-24 15:00", "Garrafa Térmica Inox 500ml",
                        "GARRAFA-INOX-500", null, null, 69.90, 59.90, 1, 5, 21.94, 12.00, 0, "Cliente C", "(11) 90000-0003", "Rua C, 3")
                .linha("2609220003", "Enviado", "2026-09-22 15:00", "2026-09-24 15:00", "Organizador de Gaveta",
                        "ORGANIZADOR-GAVETA", null, null, 29.90, 24.90, 2, 5, 21.94, 12.00, 0, "Cliente C", "(11) 90000-0003", "Rua C, 3")
                .linha("2609280004", "A Enviar", "2026-09-28 16:00", "2026-09-30 10:00", "Capa para Notebook 15,6 polegadas",
                        "CAPA-NOTEBOOK-15", null, null, 69.90, 69.90, 1, 0, 13.98, 4.00, 0, "Cliente D", "(11) 90000-0004", "Rua D, 4")
                .linha("2609250005", "Cancelado", "2026-09-25 09:00", null, "Garrafa Térmica Inox 500ml",
                        "GARRAFA-INOX-500", null, null, 69.90, 59.90, 1, 0, 0, 0, 0, "Cliente E", "(11) 90000-0005", "Rua E, 5")
                .bytes();
    }

    private JsonNode importar(byte[] planilha) throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "Order.all.20260929.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", planilha);
        return corpo(executar(multipart("/api/pedidos/importacoes").file(arquivo), token).andExpect(status().isOk()));
    }

    private void cadastrar(String sku, String nome, String custo, int estoque, int minimo) throws Exception {
        executar(post("/api/produtos").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                "sku", sku, "nome", nome, "custoUnitario", new BigDecimal(custo), "estoqueInicial", estoque,
                "estoqueMinimo", minimo))), token)
                .andExpect(status().isCreated());
    }

    private void cadastrarEsperando(String sku, int statusEsperado) throws Exception {
        executar(post("/api/produtos").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                "sku", sku, "nome", "Repetido", "custoUnitario", 1, "estoqueInicial", 1))), token)
                .andExpect(status().is(statusEsperado));
    }
}
