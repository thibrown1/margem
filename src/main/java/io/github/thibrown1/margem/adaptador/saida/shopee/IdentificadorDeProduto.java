package io.github.thibrown1.margem.adaptador.saida.shopee;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Muito vendedor pequeno não preenche SKU nos anúncios. Nesses casos o sistema gera um identificador
 * estável a partir do nome do produto e da variação, e é esse código que deve ser usado no cadastro do produto.
 */
public final class IdentificadorDeProduto {

    private static final int TAMANHO_MAXIMO = 60;

    private IdentificadorDeProduto() {
    }

    public static String escolher(String skuDaVariacao, String skuPrincipal, String nome, String variacao) {
        if (skuDaVariacao != null && !skuDaVariacao.isBlank()) {
            return skuDaVariacao.trim();
        }
        if (skuPrincipal != null && !skuPrincipal.isBlank()) {
            return skuPrincipal.trim();
        }
        return aPartirDoNome(nome, variacao);
    }

    public static String aPartirDoNome(String nome, String variacao) {
        String base = (nome == null ? "" : nome) + (variacao == null || variacao.isBlank() ? "" : " " + variacao);
        String semAcento = Normalizer.normalize(base, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String codigo = semAcento.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (codigo.isEmpty()) {
            return "SEM-SKU";
        }
        return codigo.length() > TAMANHO_MAXIMO ? codigo.substring(0, TAMANHO_MAXIMO).replaceAll("-+$", "") : codigo;
    }

    public static String descricao(String nome, String variacao) {
        String texto = (nome == null ? "" : nome.trim())
                + (variacao == null || variacao.isBlank() ? "" : " - " + variacao.trim());
        return texto.length() > 255 ? texto.substring(0, 255) : texto;
    }
}
