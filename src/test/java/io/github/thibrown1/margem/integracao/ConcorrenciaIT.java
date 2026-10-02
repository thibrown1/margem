package io.github.thibrown1.margem.integracao;

import io.github.thibrown1.margem.adaptador.saida.persistencia.entidade.ProdutoEntity;
import io.github.thibrown1.margem.adaptador.saida.persistencia.springdata.SpringDataProdutoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Duas operações leem o mesmo produto ao mesmo tempo (ex.: uma importação e uma entrada de estoque).
 * A primeira a gravar vence; a segunda recebe erro em vez de sobrescrever o estoque sem perceber.
 */
class ConcorrenciaIT extends TesteDeIntegracao {

    @Autowired
    private SpringDataProdutoRepository produtos;

    @Test
    void segundaGravacaoComDadoDesatualizadoFalha() {
        ProdutoEntity novo = new ProdutoEntity("GARRAFA-INOX-500", AGORA);
        novo.setNome("Garrafa");
        novo.setCustoUnitario(new BigDecimal("22.40"));
        novo.setEstoqueAtual(10);
        novo.setAtualizadoEm(AGORA);
        Long id = produtos.save(novo).getId();

        ProdutoEntity lidoPelaImportacao = produtos.findById(id).orElseThrow();
        ProdutoEntity lidoPelaEntrada = produtos.findById(id).orElseThrow();

        lidoPelaEntrada.setEstoqueAtual(40);
        produtos.save(lidoPelaEntrada);

        lidoPelaImportacao.setEstoqueAtual(9);
        assertThatThrownBy(() -> produtos.save(lidoPelaImportacao))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(produtos.findById(id).orElseThrow().getEstoqueAtual()).isEqualTo(40);
    }
}
