package io.github.thibrown1.margem.adaptador.entrada.web.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AtualizarProdutoRequest(
        @NotBlank @Size(max = 150) String nome,
        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal custoUnitario,
        @NotNull @PositiveOrZero Integer estoqueMinimo) {
}
