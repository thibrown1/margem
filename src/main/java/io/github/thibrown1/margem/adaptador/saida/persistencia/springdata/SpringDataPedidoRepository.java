package io.github.thibrown1.margem.adaptador.saida.persistencia.springdata;

import io.github.thibrown1.margem.adaptador.saida.persistencia.entidade.PedidoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataPedidoRepository extends JpaRepository<PedidoEntity, Long> {

    Optional<PedidoEntity> findByCodigo(String codigo);

    List<PedidoEntity> findByCodigoIn(Collection<String> codigos);

    @Query("""
            select p from PedidoEntity p
            left join fetch p.itens
            where p.criadoEm >= :inicio and p.criadoEm < :fim
            order by p.criadoEm
            """)
    List<PedidoEntity> buscarComItensCriadosEntre(@Param("inicio") Instant inicio, @Param("fim") Instant fim);

    @Query("""
            select p from PedidoEntity p
            where p.criadoEm >= :inicio and p.criadoEm < :fim
            and (:status is null or p.status = :status)
            """)
    Page<PedidoEntity> filtrar(@Param("status") String status, @Param("inicio") Instant inicio,
                               @Param("fim") Instant fim, Pageable paginacao);

    @Query("""
            select p from PedidoEntity p
            where p.status = :status
            order by case when p.prazoEnvio is null then 1 else 0 end, p.prazoEnvio, p.criadoEm
            """)
    List<PedidoEntity> buscarPorStatusOrdenadoPorPrazo(@Param("status") String status);

    @Modifying
    @Query("""
            update ItemPedidoEntity i set i.custoUnitario = :custo
            where i.sku = :sku and i.custoUnitario is null
            """)
    int preencherCustoPendente(@Param("sku") String sku, @Param("custo") BigDecimal custo);
}
