package io.github.thibrown1.margem.adaptador.saida.shopee;

/** Falha ao falar com a Shopee. Vira HTTP 502 na API. */
public class ShopeeIntegracaoException extends RuntimeException {

    public ShopeeIntegracaoException(String mensagem) {
        super(mensagem);
    }

    public ShopeeIntegracaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
