package io.github.thibrown1.margem.adaptador.entrada.web;

import io.github.thibrown1.margem.adaptador.entrada.planilha.PlanilhaInvalidaException;
import io.github.thibrown1.margem.adaptador.saida.notificacao.NotificacaoException;
import io.github.thibrown1.margem.adaptador.saida.shopee.ShopeeIntegracaoException;
import io.github.thibrown1.margem.aplicacao.ConflitoException;
import io.github.thibrown1.margem.aplicacao.RecursoNaoEncontradoException;
import io.github.thibrown1.margem.dominio.RegraDeNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/** Todos os erros da API saem no formato Problem Details (RFC 9457), com mensagem em português. */
@RestControllerAdvice
public class TratadorDeErros extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Não encontrado", e.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    public ProblemDetail conflito(ConflitoException e) {
        return problema(HttpStatus.CONFLICT, "Conflito", e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridade(DataIntegrityViolationException e) {
        log.warn("Violação de integridade: {}", e.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "Conflito", "O registro conflita com dados que já existem (ex.: SKU ou pedido repetido).");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail concorrencia(OptimisticLockingFailureException e) {
        return problema(HttpStatus.CONFLICT, "Alteração simultânea",
                "Outra operação alterou os mesmos dados ao mesmo tempo. Tente de novo.");
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ProblemDetail regraDeNegocio(RegraDeNegocioException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio", e.getMessage());
    }

    @ExceptionHandler(PlanilhaInvalidaException.class)
    public ProblemDetail planilhaInvalida(PlanilhaInvalidaException e) {
        return problema(HttpStatus.BAD_REQUEST, "Planilha inválida", e.getMessage());
    }

    @ExceptionHandler({ShopeeIntegracaoException.class, NotificacaoException.class})
    public ProblemDetail integracaoExterna(RuntimeException e) {
        log.error("Falha em integração externa", e);
        return problema(HttpStatus.BAD_GATEWAY, "Falha em serviço externo", e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail autenticacao(AuthenticationException e) {
        return problema(HttpStatus.UNAUTHORIZED, "Não autenticado", "Usuário ou senha inválidos.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail tipoInvalido(MethodArgumentTypeMismatchException e) {
        return problema(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                "Valor inválido para '" + e.getName() + "': " + e.getValue());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Algo deu errado do nosso lado. Tente de novo.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException e,
                                                                  @NonNull HttpHeaders cabecalhos,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest requisicao) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos", "Confira os campos destacados.");
        problema.setProperty("campos", campos);
        return ResponseEntity.badRequest().body(problema);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(@NonNull HandlerMethodValidationException e,
                                                                            @NonNull HttpHeaders cabecalhos,
                                                                            @NonNull HttpStatusCode status,
                                                                            @NonNull WebRequest requisicao) {
        Map<String, String> parametros = new LinkedHashMap<>();
        e.getAllValidationResults().forEach(resultado -> parametros.putIfAbsent(
                resultado.getMethodParameter().getParameterName(),
                resultado.getResolvableErrors().isEmpty() ? "valor inválido"
                        : resultado.getResolvableErrors().getFirst().getDefaultMessage()));
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Parâmetros inválidos", "Confira os parâmetros destacados.");
        problema.setProperty("campos", parametros);
        return ResponseEntity.badRequest().body(problema);
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }
}
