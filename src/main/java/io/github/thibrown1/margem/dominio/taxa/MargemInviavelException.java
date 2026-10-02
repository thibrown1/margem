package io.github.thibrown1.margem.dominio.taxa;

import io.github.thibrown1.margem.dominio.RegraDeNegocioException;

public class MargemInviavelException extends RegraDeNegocioException {

    public MargemInviavelException(String mensagem) {
        super(mensagem);
    }
}
