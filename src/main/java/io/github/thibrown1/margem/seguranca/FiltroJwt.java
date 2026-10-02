package io.github.thibrown1.margem.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Lê o "Authorization: Bearer ..." e, se o token for válido, autentica a requisição. */
public class FiltroJwt extends OncePerRequestFilter {

    static final String ATRIBUTO_ERRO_TOKEN = "margem.erroToken";
    private static final String PREFIXO = "Bearer ";

    private final JwtService jwt;

    public FiltroJwt(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        String cabecalho = requisicao.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho != null && cabecalho.startsWith(PREFIXO)) {
            jwt.validar(cabecalho.substring(PREFIXO.length()).trim()).ifPresentOrElse(
                    usuario -> {
                        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
                        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(usuario.login(), null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.perfil().name()))));
                        SecurityContextHolder.setContext(contexto);
                    },
                    () -> requisicao.setAttribute(ATRIBUTO_ERRO_TOKEN, "Token inválido ou expirado. Faça login de novo."));
        }
        cadeia.doFilter(requisicao, resposta);
    }
}
