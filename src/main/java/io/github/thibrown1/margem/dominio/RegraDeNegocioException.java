package io.github.thibrown1.margem.dominio;

/** Violação de uma regra do negócio. Vira HTTP 422 na API. */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
