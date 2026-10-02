package io.github.thibrown1.margem.adaptador.entrada.web;

import io.github.thibrown1.margem.aplicacao.produto.ItemEstoque;
import io.github.thibrown1.margem.aplicacao.produto.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/estoque")
@Tag(name = "3. Estoque")
public class EstoqueController {

    private final ProdutoService produtos;

    public EstoqueController(ProdutoService produtos) {
        this.produtos = produtos;
    }

    @GetMapping
    @Operation(summary = "Estoque do mais urgente para o mais tranquilo", description = "Sem valores financeiros. DONO e EXPEDICAO.")
    public List<ItemEstoque> estoque() {
        return produtos.estoque();
    }
}
