package io.github.thibrown1.margem.adaptador.entrada.planilha;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Colunas que o sistema lê da planilha de pedidos da Central do Vendedor.
 * <p>
 * Cada coluna aceita vários nomes (a Shopee muda o cabeçalho entre versões e idiomas). A comparação ignora acentos,
 * maiúsculas e pontuação, e aceita sufixos como "(R$)". Se a Shopee mudar o layout, é só incluir o novo nome aqui.
 * <p>
 * Colunas de dados pessoais do comprador (nome, telefone, endereço, CPF) não estão nesta lista de propósito:
 * elas nunca são lidas nem gravadas (LGPD).
 */
public enum ColunaPlanilha {

    CODIGO(true, "id do pedido", "numero do pedido", "n do pedido", "order id", "order sn"),
    STATUS(true, "status do pedido", "order status"),
    CRIADO_EM(true, "data de criacao do pedido", "hora de criacao do pedido", "data do pedido", "order creation date"),
    PRAZO_ENVIO(false, "data prevista de envio", "prazo de envio", "enviar ate", "ship by date"),
    SKU_VARIACAO(false, "numero de referencia sku", "n de referencia sku", "no de referencia sku", "sku da variacao",
            "sku reference no", "variation sku"),
    SKU_PRINCIPAL(false, "n de referencia do sku principal", "numero de referencia do sku principal",
            "no de referencia do sku principal", "sku principal", "sku do produto", "parent sku reference no", "parent sku"),
    NOME_PRODUTO(true, "nome do produto", "product name"),
    NOME_VARIACAO(false, "nome da variacao", "variation name"),
    QUANTIDADE(true, "quantidade", "quantity"),
    PRECO_ACORDADO(false, "preco acordado", "deal price", "preco de venda"),
    PRECO_ORIGINAL(false, "preco original", "original price"),
    CUPOM_VENDEDOR(false, "cupom do vendedor", "seller voucher", "cupom da loja"),
    TAXA_COMISSAO(false, "taxa de comissao", "commission fee"),
    TAXA_SERVICO(false, "taxa de servico", "service fee"),
    TAXA_TRANSACAO(false, "taxa de transacao", "transaction fee");

    private final boolean obrigatoria;
    private final List<String> nomes;

    ColunaPlanilha(boolean obrigatoria, String... nomes) {
        this.obrigatoria = obrigatoria;
        this.nomes = List.of(nomes);
    }

    public boolean isObrigatoria() {
        return obrigatoria;
    }

    public String nomePrincipal() {
        return nomes.getFirst();
    }

    public boolean reconhece(String cabecalho, boolean somenteNomeExato) {
        String normalizado = normalizar(cabecalho);
        return nomes.stream().anyMatch(nome -> normalizado.equals(nome)
                || (!somenteNomeExato && normalizado.startsWith(nome + " ")));
    }

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
