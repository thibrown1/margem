package io.github.thibrown1.margem.adaptador.entrada.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record PrecoIdealRequest(
        @Schema(example = "22.40") @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal custoProduto,
        @Schema(description = "Margem de lucro desejada, em %", example = "20")
        @NotNull @DecimalMin("0") @DecimalMax(value = "100", inclusive = false) BigDecimal margemDesejada) {
}
