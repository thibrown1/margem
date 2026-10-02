package io.github.thibrown1.margem.seguranca;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/** Responde 401 e 403 no mesmo formato (RFC 9457 Problem Details) usado pelo resto da API. */
@Component
public class TratadorErrosSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper json;

    public TratadorErrosSeguranca(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public void commence(HttpServletRequest requisicao, HttpServletResponse resposta, AuthenticationException erro)
            throws IOException {
        Object erroDoToken = requisicao.getAttribute(FiltroJwt.ATRIBUTO_ERRO_TOKEN);
        String detalhe = erroDoToken != null
                ? erroDoToken.toString()
                : "Faça login em POST /api/auth/login e envie o token no cabeçalho Authorization: Bearer <token>.";
        escrever(requisicao, resposta, HttpStatus.UNAUTHORIZED, "Não autenticado", detalhe);
    }

    @Override
    public void handle(HttpServletRequest requisicao, HttpServletResponse resposta, AccessDeniedException erro)
            throws IOException {
        escrever(requisicao, resposta, HttpStatus.FORBIDDEN, "Acesso negado", "Seu perfil não tem acesso a este recurso.");
    }

    private void escrever(HttpServletRequest requisicao, HttpServletResponse resposta, HttpStatus status,
                          String titulo, String detalhe) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setInstance(URI.create(requisicao.getRequestURI()));
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        json.writeValue(resposta.getOutputStream(), problema);
    }
}
