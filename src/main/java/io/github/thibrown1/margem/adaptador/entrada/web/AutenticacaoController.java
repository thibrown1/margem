package io.github.thibrown1.margem.adaptador.entrada.web;

import io.github.thibrown1.margem.adaptador.entrada.web.dto.LoginRequest;
import io.github.thibrown1.margem.adaptador.entrada.web.dto.TokenResponse;
import io.github.thibrown1.margem.seguranca.AutenticacaoService;
import io.github.thibrown1.margem.seguranca.TokenGerado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "1. Autenticação")
public class AutenticacaoController {

    private final AutenticacaoService autenticacao;
    private final Clock relogio;

    public AutenticacaoController(AutenticacaoService autenticacao, Clock relogio) {
        this.autenticacao = autenticacao;
        this.relogio = relogio;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Faz login e devolve um token JWT",
            description = "Usuários da demonstração: dono / dono123 (vê tudo) e expedicao / expedicao123 (só envio e estoque).")
    public TokenResponse login(@Valid @RequestBody LoginRequest dados) {
        TokenGerado token = autenticacao.login(dados.usuario(), dados.senha());
        return new TokenResponse(token.token(), "Bearer", token.expiraEm().atZone(relogio.getZone()).toOffsetDateTime(),
                token.perfil().name());
    }
}
