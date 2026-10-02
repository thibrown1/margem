package io.github.thibrown1.margem.seguranca;

import java.time.Instant;

public record TokenGerado(String token, Instant expiraEm, Perfil perfil) {
}
