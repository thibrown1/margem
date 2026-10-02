package io.github.thibrown1.margem.aplicacao;

/** O recurso pedido não existe. Vira HTTP 404 na API. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
