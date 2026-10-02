package io.github.thibrown1.margem.aplicacao;

/** A operação conflita com algo que já existe (ex.: SKU repetido). Vira HTTP 409 na API. */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
