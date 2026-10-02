package io.github.thibrown1.margem.aplicacao.produto;

import io.github.thibrown1.margem.aplicacao.ConflitoException;
import io.github.thibrown1.margem.aplicacao.RecursoNaoEncontradoException;
import io.github.thibrown1.margem.aplicacao.porta.PedidoRepositorio;
import io.github.thibrown1.margem.aplicacao.porta.ProdutoRepositorio;
import io.github.thibrown1.margem.dominio.produto.Produto;
import io.github.thibrown1.margem.dominio.produto.SituacaoEstoque;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepositorio produtos;

    @Mock
    private PedidoRepositorio pedidos;

    @InjectMocks
    private ProdutoService servico;

    @Test
    void naoPermiteSkuRepetido() {
        when(produtos.existePorSku("GARRAFA-INOX-500")).thenReturn(true);

        assertThatThrownBy(() -> servico.cadastrar(new NovoProduto("garrafa-inox-500", "Garrafa", BigDecimal.TEN, 1, 0)))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("GARRAFA-INOX-500");
        verify(produtos, never()).salvar(any());
    }

    @Test
    void cadastroPreencheOCustoDeVendasAntigasDoMesmoSku() {
        when(produtos.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));
        when(pedidos.preencherCustoPendente("CAPA-NOTEBOOK-15", new BigDecimal("28.50"))).thenReturn(3);

        ProdutoCadastrado cadastrado = servico.cadastrar(
                new NovoProduto("capa-notebook-15", "Capa", new BigDecimal("28.5"), 20, 5));

        assertThat(cadastrado.itensDePedidosAtualizados()).isEqualTo(3);
        assertThat(cadastrado.produto().getSku()).isEqualTo("CAPA-NOTEBOOK-15");
    }

    @Test
    void entradaEmProdutoInexistenteDaErro() {
        when(produtos.buscarPorSku("NAO-EXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.registrarEntrada("nao-existe", 5, BigDecimal.ONE))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void entradaRecalculaCustoMedioESalva() {
        Produto produto = new Produto(1L, "GARRAFA", "Garrafa", new BigDecimal("20.00"), 10, 0);
        when(produtos.buscarPorSku("GARRAFA")).thenReturn(Optional.of(produto));
        when(produtos.salvar(produto)).thenReturn(produto);

        Produto atualizado = servico.registrarEntrada("garrafa", 10, new BigDecimal("30.00"));

        assertThat(atualizado.getCustoUnitario()).isEqualByComparingTo("25.00");
        verify(produtos).salvar(produto);
    }

    @Test
    void estoqueVemDoMaisUrgenteParaOMaisTranquilo() {
        when(produtos.listarTodos()).thenReturn(List.of(
                new Produto(1L, "OK", "Ok", BigDecimal.ONE, 50, 5),
                new Produto(2L, "ZERADO", "Zerado", BigDecimal.ONE, 0, 5),
                new Produto(3L, "BAIXO", "Baixo", BigDecimal.ONE, 3, 5)));

        assertThat(servico.estoque()).extracting(ItemEstoque::situacao)
                .containsExactly(SituacaoEstoque.SEM_ESTOQUE, SituacaoEstoque.BAIXO, SituacaoEstoque.OK);
    }
}
