package io.github.thibrown1.margem.adaptador.entrada.planilha;

/** A planilha inteira não pode ser lida (formato errado, colunas obrigatórias faltando). Vira HTTP 400. */
public class PlanilhaInvalidaException extends RuntimeException {

    public PlanilhaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
