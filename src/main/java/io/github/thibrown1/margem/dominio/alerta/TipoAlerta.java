package io.github.thibrown1.margem.dominio.alerta;

/** A ordem das constantes é a ordem em que as seções aparecem na mensagem. */
public enum TipoAlerta {
    PRAZO_ENVIO("📦", "Envio perto do prazo"),
    ESTOQUE_BAIXO("📉", "Estoque baixo"),
    PEDIDO_NO_PREJUIZO("💸", "Pedidos que deram prejuízo");

    private final String icone;
    private final String titulo;

    TipoAlerta(String icone, String titulo) {
        this.icone = icone;
        this.titulo = titulo;
    }

    public String getIcone() {
        return icone;
    }

    public String getTitulo() {
        return titulo;
    }
}
