package io.github.thibrown1.margem.adaptador.saida.persistencia.springdata;

import io.github.thibrown1.margem.adaptador.saida.persistencia.entidade.ProdutoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataProdutoRepository extends JpaRepository<ProdutoEntity, Long> {

    Optional<ProdutoEntity> findBySku(String sku);

    List<ProdutoEntity> findBySkuIn(Collection<String> skus);

    boolean existsBySku(String sku);
}
