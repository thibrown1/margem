package io.github.thibrown1.margem.adaptador.entrada.web.dto;

import java.time.OffsetDateTime;

public record TokenResponse(String token, String tipo, OffsetDateTime expiraEm, String perfil) {
}
