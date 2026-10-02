package io.github.thibrown1.margem.adaptador.entrada.web;

import io.github.thibrown1.margem.adaptador.entrada.web.dto.AtualizarProdutoRequest;
import io.github.thibrown1.margem.adaptador.entrada.web.dto.EntradaEstoqueRequest;
import io.github.thibrown1.margem.adaptador.entrada.web.dto.NovoProdutoRequest;
import io.github.thibrown1.margem.adaptador.entrada.web.dto.ProdutoCadastradoResponse;
import io.github.thibrown1.margem.adaptador.entrada.web.dto.ProdutoResponse;
import io.github.thibrown1.margem.aplicacao.produto.NovoProduto;
import io.github.thibrown1.margem.aplicacao.produto.ProdutoCadastrado;
import io.github.thibrown1.margem.aplicacao.produto.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/produtos")
@Tag(name = "2. Produtos", description = "Catálogo com custo e estoque. O SKU liga o produto aos itens vendidos na Shopee.")
public class ProdutoController {

    private final ProdutoService produtos;

    public ProdutoController(ProdutoService produtos) {
        this.produtos = produtos;
    }

    @PostMapping
    @Operation(summary = "Cadastra um produto",
            description = "Se já existirem itens vendidos com esse SKU sem custo, eles recebem o custo agora.")
    public ResponseEntity<ProdutoCadastradoResponse> cadastrar(@Valid @RequestBody NovoProdutoRequest dados) {
        ProdutoCadastrado cadastrado = produtos.cadastrar(new NovoProduto(dados.sku(), dados.nome(), dados.custoUnitario(),
                dados.estoqueInicial(), dados.estoqueMinimo() == null ? 0 : dados.estoqueMinimo()));
        ProdutoResponse produto = ProdutoResponse.de(cadastrado.produto());
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{sku}").buildAndExpand(produto.sku()).toUri())
                .body(new ProdutoCadastradoResponse(produto, cadastrado.itensDePedidosAtualizados()));
    }

    @GetMapping
    @Operation(summary = "Lista os produtos com custo e estoque")
    public List<ProdutoResponse> listar() {
        return produtos.listar().stream().map(ProdutoResponse::de).toList();
    }

    @GetMapping("/{sku}")
    @Operation(summary = "Busca um produto pelo SKU")
    public ProdutoResponse buscar(@PathVariable String sku) {
        return ProdutoResponse.de(produtos.buscar(sku));
    }

    @PutMapping("/{sku}")
    @Operation(summary = "Atualiza nome, custo e estoque mínimo",
            description = "O novo custo vale para as próximas vendas; pedidos já importados mantêm o custo da época.")
    public ProdutoResponse atualizar(@PathVariable String sku, @Valid @RequestBody AtualizarProdutoRequest dados) {
        return ProdutoResponse.de(produtos.atualizar(sku, dados.nome(), dados.custoUnitario(), dados.estoqueMinimo()));
    }

    @PostMapping("/{sku}/entradas")
    @Operation(summary = "Registra chegada de mercadoria", description = "Soma ao estoque e recalcula o custo médio ponderado.")
    public ProdutoResponse registrarEntrada(@PathVariable String sku, @Valid @RequestBody EntradaEstoqueRequest dados) {
        return ProdutoResponse.de(produtos.registrarEntrada(sku, dados.quantidade(), dados.custoUnitario()));
    }
}
