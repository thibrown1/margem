package io.github.thibrown1.margem.adaptador.entrada.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "dono") @NotBlank String usuario,
        @Schema(example = "dono123") @NotBlank String senha) {
}
