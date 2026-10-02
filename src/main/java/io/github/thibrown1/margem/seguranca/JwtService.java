package io.github.thibrown1.margem.seguranca;

import io.github.thibrown1.margem.configuracao.MargemProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Gera e valida tokens JWT assinados com HMAC. O perfil vai dentro do token,
 * então validar uma requisição não precisa consultar o banco.
 */
@Component
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String EMISSOR = "margem";
    private static final String CLAIM_PERFIL = "perfil";
    private static final int TAMANHO_MINIMO_DO_SEGREDO = 32;

    private final SecretKey chave;
    private final Duration validade;
    private final Clock relogio;

    public JwtService(MargemProperties propriedades, Clock relogio) {
        MargemProperties.Seguranca seguranca = propriedades.seguranca();
        this.chave = criarChave(seguranca.jwtSegredo());
        this.validade = Duration.ofMinutes(seguranca.jwtValidadeMinutos());
        this.relogio = relogio;
    }

    public TokenGerado gerar(String login, Perfil perfil) {
        Instant agora = relogio.instant();
        Instant expiraEm = agora.plus(validade);
        String token = Jwts.builder()
                .issuer(EMISSOR)
                .subject(login)
                .claim(CLAIM_PERFIL, perfil.name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiraEm))
                .signWith(chave)
                .compact();
        return new TokenGerado(token, expiraEm, perfil);
    }

    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .requireIssuer(EMISSOR)
                    .clock(() -> Date.from(relogio.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new UsuarioAutenticado(claims.getSubject(),
                    Perfil.valueOf(claims.get(CLAIM_PERFIL, String.class))));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    private static SecretKey criarChave(String segredo) {
        if (segredo == null || segredo.isBlank()) {
            byte[] aleatorio = new byte[64];
            new SecureRandom().nextBytes(aleatorio);
            log.warn("MARGEM_JWT_SEGREDO não foi definido. Gerei uma chave temporária: os tokens deixam de valer"
                    + " quando a aplicação reinicia.");
            return Keys.hmacShaKeyFor(aleatorio);
        }
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO_DO_SEGREDO) {
            throw new IllegalStateException("MARGEM_JWT_SEGREDO precisa ter pelo menos " + TAMANHO_MINIMO_DO_SEGREDO + " caracteres");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
