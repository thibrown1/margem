package io.github.thibrown1.margem.adaptador.entrada.planilha;

import io.github.thibrown1.margem.aplicacao.pedido.PedidoImportado;
import io.github.thibrown1.margem.dominio.pedido.StatusPedido;
import io.github.thibrown1.margem.suporte.PlanilhaShopeeDeTeste;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeitorPlanilhaShopeeTest {

    private final LeitorPlanilhaShopee leitor =
            new LeitorPlanilhaShopee(Clock.fixed(Instant.parse("2026-09-29T15:00:00Z"), ZoneId.of("America/Sao_Paulo")));

    @Test
    void agrupaAsLinhasDoMesmoPedido() {
        byte[] planilha = PlanilhaShopeeDeTeste.padrao()
                .linha("260920ABC", "Concluído", "2026-09-20 14:30", "2026-09-22 14:30", "Garrafa Térmica Inox 500ml",
                        "GARRAFA-INOX-500", "GARRAFA", "Preta", 69.90, 59.90, 1, 5, 33.94, 12.00, 0,
                        "Maria da Silva", "(11) 99999-0000", "Rua das Flores, 123")
                .linha("260920ABC", "Concluído", "2026-09-20 14:30", "2026-09-22 14:30", "Organizador de Gaveta",
                        "ORGANIZADOR-GAVETA", "", "", 29.90, 24.90, 2, 5, 33.94, 12.00, 0,
                        "Maria da Silva", "(11) 99999-0000", "Rua das Flores, 123")
                .bytes();

        LeituraPlanilha leitura = ler(planilha);

        assertThat(leitura.erros()).isEmpty();
        PedidoImportado pedido = leitura.pedidos().getFirst();
        assertThat(pedido.codigo()).isEqualTo("260920ABC");
        assertThat(pedido.status()).isEqualTo(StatusPedido.CONCLUIDO);
        assertThat(pedido.criadoEm()).isEqualTo(Instant.parse("2026-09-20T17:30:00Z"));
        assertThat(pedido.descontoVendedor()).isEqualByComparingTo("5.00");
        assertThat(pedido.taxas().total()).as("taxas do pedido lidas uma vez, não somadas por linha").isEqualByComparingTo("45.94");
        assertThat(pedido.itens()).hasSize(2);
        assertThat(pedido.itens().getFirst().descricao()).isEqualTo("Garrafa Térmica Inox 500ml - Preta");
        assertThat(pedido.itens().get(1).quantidade()).isEqualTo(2);
        assertThat(pedido.itens().get(1).precoUnitario()).isEqualByComparingTo("24.90");
    }

    @Test
    void naoGuardaDadosPessoaisDoComprador() {
        byte[] planilha = PlanilhaShopeeDeTeste.padrao()
                .linha("P1", "Concluído", "2026-09-20 14:30", null, "Garrafa", "GARRAFA", null, null, 59.90, 59.90, 1,
                        0, 11.98, 4, 0, "Maria da Silva", "(11) 99999-0000", "Rua das Flores, 123")
                .bytes();

        assertThat(ler(planilha).toString())
                .doesNotContain("Maria")
                .doesNotContain("99999")
                .doesNotContain("Rua das Flores");
    }

    @Test
    void aceitaValoresEDatasEscritosComoTexto() {
        byte[] planilha = PlanilhaShopeeDeTeste.padrao()
                .linha("P1", "A Enviar", "29/09/2026 09:15", "01/10/2026 09:15", "Luminária", "LUMINARIA-LED", null, null,
                        "R$ 99,90", "R$ 1.084,90", "1", "0", "R$ 151,89", "20,00", "0", null, null, null)
                .bytes();

        PedidoImportado pedido = ler(planilha).pedidos().getFirst();

        assertThat(pedido.status()).isEqualTo(StatusPedido.AGUARDANDO_ENVIO);
        assertThat(pedido.criadoEm()).isEqualTo(Instant.parse("2026-09-29T12:15:00Z"));
        assertThat(pedido.prazoEnvio()).isEqualTo(Instant.parse("2026-10-01T12:15:00Z"));
        assertThat(pedido.itens().getFirst().precoUnitario()).isEqualByComparingTo("1084.90");
        assertThat(pedido.taxas().comissao()).isEqualByComparingTo("151.89");
    }

    @Test
    void semSkuUsaOSkuPrincipalOuONomeDoProduto() {
        byte[] planilha = PlanilhaShopeeDeTeste.padrao()
                .linha("P1", "Concluído", "2026-09-20 14:30", null, "Garrafa", "", "GARRAFA-PAI", null, 59.90, 59.90, 1,
                        0, 0, 0, 0, null, null, null)
                .linha("P2", "Concluído", "2026-09-20 14:30", null, "Capa para Notebook 15,6\"", "", "", "Azul", 69.90, 69.90,
                        1, 0, 0, 0, 0, null, null, null)
                .bytes();

        LeituraPlanilha leitura = ler(planilha);

        assertThat(leitura.pedidos().get(0).itens().getFirst().sku()).isEqualTo("GARRAFA-PAI");
        assertThat(leitura.pedidos().get(1).itens().getFirst().sku()).isEqualTo("CAPA-PARA-NOTEBOOK-15-6-AZUL");
    }

    @Test
    void linhaComErroTiraSoAquelePedidoEDizALinha() {
        byte[] planilha = PlanilhaShopeeDeTeste.padrao()
                .linha("BOM", "Concluído", "2026-09-20 14:30", null, "Garrafa", "GARRAFA", null, null, 59.90, 59.90, 1,
                        0, 11.98, 4, 0, null, null, null)
                .linha("RUIM", "Concluído", "2026-09-20 14:30", null, "Garrafa", "GARRAFA", null, null, 59.90, 59.90, "dois",
                        0, 11.98, 4, 0, null, null, null)
                .bytes();

        LeituraPlanilha leitura = ler(planilha);

        assertThat(leitura.pedidos()).extracting(PedidoImportado::codigo).containsExactly("BOM");
        assertThat(leitura.erros()).singleElement().satisfies(erro -> {
            assertThat(erro.linha()).isEqualTo(3);
            assertThat(erro.pedido()).isEqualTo("RUIM");
        });
    }

    @Test
    void semColunasDeTaxaAsTaxasFicamParaEstimar() {
        byte[] planilha = PlanilhaShopeeDeTeste.comCabecalho("ID do pedido", "Status do pedido", "Data de criação do pedido",
                        "Nome do Produto", "SKU principal", "Preço acordado (R$)", "Quantidade")
                .linha("P1", "Concluído", "2026-09-20 14:30", "Garrafa", "GARRAFA", 59.90, 1)
                .bytes();

        PedidoImportado pedido = ler(planilha).pedidos().getFirst();

        assertThat(pedido.taxas()).isNull();
        assertThat(pedido.itens().getFirst().precoUnitario()).as("aceita sufixo no nome da coluna").isEqualByComparingTo("59.90");
    }

    @Test
    void avisaQuaisColunasObrigatoriasFaltam() {
        byte[] planilha = PlanilhaShopeeDeTeste.comCabecalho("ID do pedido", "Nome do Produto").linha("P1", "Garrafa").bytes();

        assertThatThrownBy(() -> ler(planilha))
                .isInstanceOf(PlanilhaInvalidaException.class)
                .hasMessageContaining("status do pedido")
                .hasMessageContaining("quantidade");
    }

    @Test
    void recusaArquivoQueNaoEhPlanilha() {
        assertThatThrownBy(() -> leitor.ler(new ByteArrayInputStream("não sou uma planilha".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(PlanilhaInvalidaException.class);
    }

    private LeituraPlanilha ler(byte[] planilha) {
        return leitor.ler(new ByteArrayInputStream(planilha));
    }
}
