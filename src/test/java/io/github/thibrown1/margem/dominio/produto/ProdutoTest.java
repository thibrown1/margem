package io.github.thibrown1.margem.dominio.produto;

import io.github.thibrown1.margem.dominio.RegraDeNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProdutoTest {

    @Test
    void entradaDeMercadoriaRecalculaCustoMedioPonderado() {
        Produto produto = Produto.novo("garrafa", "Garrafa", new BigDecimal("20.00"), 10, 0);

        produto.registrarEntrada(10, new BigDecimal("30.00"));

        assertThat(produto.getCustoUnitario()).isEqualByComparingTo("25.00");
        assertThat(produto.getEstoqueAtual()).isEqualTo(20);
    }

    @Test
    void comEstoqueZeradoOCustoPassaASerODaCompraNova() {
        Produto produto = Produto.novo("GARRAFA", "Garrafa", new BigDecimal("20.00"), 0, 0);

        produto.registrarEntrada(5, new BigDecimal("18.37"));

        assertThat(produto.getCustoUnitario()).isEqualByComparingTo("18.37");
        assertThat(produto.getEstoqueAtual()).isEqualTo(5);
    }

    @Test
    void comEstoqueNegativoOCustoAntigoNaoEntraNaMedia() {
        Produto produto = new Produto(1L, "GARRAFA", "Garrafa", new BigDecimal("50.00"), -2, 0);

        produto.registrarEntrada(5, new BigDecimal("10.00"));

        assertThat(produto.getCustoUnitario()).isEqualByComparingTo("10.00");
        assertThat(produto.getEstoqueAtual()).isEqualTo(3);
    }

    @Test
    void custoMedioEhArredondadoEmCentavos() {
        Produto produto = Produto.novo("GARRAFA", "Garrafa", new BigDecimal("10.00"), 3, 0);

        produto.registrarEntrada(1, new BigDecimal("10.01"));

        assertThat(produto.getCustoUnitario()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void vendaPodeDeixarEstoqueNegativoECancelamentoDevolve() {
        Produto produto = Produto.novo("GARRAFA", "Garrafa", BigDecimal.TEN, 1, 0);

        produto.baixarEstoque(3);
        assertThat(produto.getEstoqueAtual()).isEqualTo(-2);

        produto.devolverAoEstoque(3);
        assertThat(produto.getEstoqueAtual()).isEqualTo(1);
    }

    @Test
    void situacaoDoEstoque() {
        assertThat(Produto.novo("A", "A", BigDecimal.ONE, 0, 5).situacaoEstoque()).isEqualTo(SituacaoEstoque.SEM_ESTOQUE);
        assertThat(Produto.novo("A", "A", BigDecimal.ONE, 5, 5).situacaoEstoque()).isEqualTo(SituacaoEstoque.BAIXO);
        assertThat(Produto.novo("A", "A", BigDecimal.ONE, 6, 5).situacaoEstoque()).isEqualTo(SituacaoEstoque.OK);
    }

    @Test
    void skuEhNormalizado() {
        assertThat(Produto.novo("  garrafa-inox-500 ", "Garrafa", BigDecimal.ONE, 1, 0).getSku()).isEqualTo("GARRAFA-INOX-500");
    }

    @Test
    void validaDados() {
        assertThatThrownBy(() -> Produto.novo("A", " ", BigDecimal.ONE, 1, 0)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> Produto.novo("A", "A", new BigDecimal("-1"), 1, 0)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> Produto.novo("A", "A", BigDecimal.ONE, -1, 0)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> Produto.novo(" ", "A", BigDecimal.ONE, 1, 0)).isInstanceOf(RegraDeNegocioException.class);
        Produto produto = Produto.novo("A", "A", BigDecimal.ONE, 1, 0);
        assertThatThrownBy(() -> produto.registrarEntrada(0, BigDecimal.ONE)).isInstanceOf(RegraDeNegocioException.class);
    }
}
