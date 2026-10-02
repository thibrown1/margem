package io.github.thibrown1.margem.adaptador.entrada.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record EntradaEstoqueRequest(
        @Schema(example = "30") @NotNull @Positive @Max(100_000) Integer quantidade,
        @Schema(description = "Custo unitário desta compra; o sistema recalcula o custo médio", example = "24.90")
        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal custoUnitario) {
}
