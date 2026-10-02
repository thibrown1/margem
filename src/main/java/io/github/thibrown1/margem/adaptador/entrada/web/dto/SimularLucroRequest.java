package io.github.thibrown1.margem.adaptador.entrada.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record SimularLucroRequest(
        @Schema(example = "84.90") @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal precoVenda,
        @Schema(example = "31.50") @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal custoProduto) {
}
