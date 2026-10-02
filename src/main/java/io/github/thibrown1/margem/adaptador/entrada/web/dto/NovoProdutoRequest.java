package io.github.thibrown1.margem.adaptador.entrada.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record NovoProdutoRequest(
        @Schema(description = "O mesmo SKU cadastrado no anúncio da Shopee", example = "CAPA-NOTEBOOK-15")
        @NotBlank @Size(max = 60) String sku,
        @Schema(example = "Capa para Notebook 15,6 polegadas") @NotBlank @Size(max = 150) String nome,
        @Schema(description = "Quanto custa cada unidade para a loja", example = "28.50")
        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal custoUnitario,
        @Schema(example = "20") @NotNull @PositiveOrZero Integer estoqueInicial,
        @Schema(description = "Abaixo disso chega alerta de estoque baixo", example = "5") @PositiveOrZero Integer estoqueMinimo) {
}
