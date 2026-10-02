package io.github.thibrown1.margem.adaptador.entrada.planilha;

import io.github.thibrown1.margem.adaptador.saida.shopee.IdentificadorDeProduto;
import io.github.thibrown1.margem.adaptador.saida.shopee.StatusShopee;
import io.github.thibrown1.margem.aplicacao.pedido.ErroImportacao;
import io.github.thibrown1.margem.aplicacao.pedido.ItemImportado;
import io.github.thibrown1.margem.aplicacao.pedido.PedidoImportado;
import io.github.thibrown1.margem.dominio.Moeda;
import io.github.thibrown1.margem.dominio.pedido.StatusPedido;
import io.github.thibrown1.margem.dominio.pedido.TaxasShopee;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Lê a planilha de pedidos exportada da Central do Vendedor (Meus Pedidos &gt; Exportar).
 * <p>
 * A planilha tem uma linha por produto; os dados do pedido (status, datas, taxas) se repetem em cada linha.
 * Por isso as linhas são agrupadas pelo código do pedido, e os valores do pedido são lidos da primeira linha.
 * Se qualquer linha de um pedido tiver erro, o pedido inteiro fica de fora e o erro é informado.
 */
@Component
public class LeitorPlanilhaShopee {

    static final int MAXIMO_DE_LINHAS = 20_000;
    private static final int LINHAS_PROCURANDO_CABECALHO = 10;
    private static final List<DateTimeFormatter> FORMATOS_DATA_HORA = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm[:ss]"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm[:ss]"));
    private static final List<DateTimeFormatter> FORMATOS_DATA = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    private final ZoneId zona;

    public LeitorPlanilhaShopee(Clock relogio) {
        this.zona = relogio.getZone();
    }

    public LeituraPlanilha ler(InputStream conteudo) {
        Workbook planilha;
        try {
            planilha = WorkbookFactory.create(conteudo);
        } catch (IOException | RuntimeException e) {
            throw new PlanilhaInvalidaException("Não consegui abrir a planilha. Envie o arquivo .xlsx exportado da Central"
                    + " do Vendedor, sem alterações.");
        }
        try (planilha) {
            if (planilha.getNumberOfSheets() == 0) {
                throw new PlanilhaInvalidaException("A planilha está vazia.");
            }
            return lerAba(planilha.getSheetAt(0));
        } catch (IOException e) {
            throw new PlanilhaInvalidaException("Não consegui ler a planilha até o fim.");
        }
    }

    private LeituraPlanilha lerAba(Sheet aba) {
        Row cabecalho = encontrarCabecalho(aba);
        Map<ColunaPlanilha, Integer> colunas = mapearColunas(cabecalho);

        Map<String, List<Row>> linhasPorPedido = new LinkedHashMap<>();
        int ultimaLinha = Math.min(aba.getLastRowNum(), cabecalho.getRowNum() + MAXIMO_DE_LINHAS);
        if (aba.getLastRowNum() > ultimaLinha) {
            throw new PlanilhaInvalidaException("A planilha tem mais de " + MAXIMO_DE_LINHAS + " linhas. Exporte um período menor.");
        }
        for (int i = cabecalho.getRowNum() + 1; i <= ultimaLinha; i++) {
            Row linha = aba.getRow(i);
            String codigo = linha == null ? "" : texto(linha, colunas.get(ColunaPlanilha.CODIGO));
            if (!codigo.isBlank()) {
                linhasPorPedido.computeIfAbsent(codigo, c -> new ArrayList<>()).add(linha);
            }
        }

        List<PedidoImportado> pedidos = new ArrayList<>();
        List<ErroImportacao> erros = new ArrayList<>();
        linhasPorPedido.forEach((codigo, linhas) -> {
            try {
                pedidos.add(montarPedido(codigo, linhas, colunas));
            } catch (ValorInvalidoException e) {
                erros.add(new ErroImportacao(e.linhaNoExcel, codigo, e.getMessage()));
            }
        });
        return new LeituraPlanilha(List.copyOf(pedidos), List.copyOf(erros));
    }

    private Row encontrarCabecalho(Sheet aba) {
        for (int i = aba.getFirstRowNum(); i <= Math.min(aba.getLastRowNum(), LINHAS_PROCURANDO_CABECALHO); i++) {
            Row linha = aba.getRow(i);
            if (linha != null && encontrarColuna(linha, ColunaPlanilha.CODIGO).isPresent()) {
                return linha;
            }
        }
        throw new PlanilhaInvalidaException("Não encontrei a coluna \"ID do pedido\". Esta é a planilha de pedidos da Shopee?");
    }

    private Map<ColunaPlanilha, Integer> mapearColunas(Row cabecalho) {
        Map<ColunaPlanilha, Integer> colunas = new EnumMap<>(ColunaPlanilha.class);
        for (ColunaPlanilha coluna : ColunaPlanilha.values()) {
            encontrarColuna(cabecalho, coluna).ifPresent(indice -> colunas.put(coluna, indice));
        }
        Set<String> faltando = new LinkedHashSet<>();
        for (ColunaPlanilha coluna : ColunaPlanilha.values()) {
            if (coluna.isObrigatoria() && !colunas.containsKey(coluna)) {
                faltando.add("\"" + coluna.nomePrincipal() + "\"");
            }
        }
        if (!colunas.containsKey(ColunaPlanilha.PRECO_ACORDADO) && !colunas.containsKey(ColunaPlanilha.PRECO_ORIGINAL)) {
            faltando.add("\"preco acordado\" ou \"preco original\"");
        }
        if (!faltando.isEmpty()) {
            throw new PlanilhaInvalidaException("Faltam colunas na planilha: " + String.join(", ", faltando));
        }
        return colunas;
    }

    /** Primeiro procura o nome exato; só depois aceita nomes com sufixo, como "Preço acordado (R$)". */
    private static Optional<Integer> encontrarColuna(Row cabecalho, ColunaPlanilha coluna) {
        for (boolean exato : new boolean[]{true, false}) {
            for (Cell celula : cabecalho) {
                if (celula.getCellType() == CellType.STRING && coluna.reconhece(celula.getStringCellValue(), exato)) {
                    return Optional.of(celula.getColumnIndex());
                }
            }
        }
        return Optional.empty();
    }

    private PedidoImportado montarPedido(String codigo, List<Row> linhas, Map<ColunaPlanilha, Integer> colunas) {
        Row primeira = linhas.getFirst();
        int linhaExcel = primeira.getRowNum() + 1;

        String textoStatus = texto(primeira, colunas.get(ColunaPlanilha.STATUS));
        StatusPedido status = StatusShopee.doTextoDaPlanilha(textoStatus)
                .orElseThrow(() -> new ValorInvalidoException(linhaExcel, "Status desconhecido: \"" + textoStatus + "\""));
        Instant criadoEm = dataHora(primeira, colunas.get(ColunaPlanilha.CRIADO_EM), linhaExcel, true);
        Instant prazoEnvio = dataHora(primeira, colunas.get(ColunaPlanilha.PRAZO_ENVIO), linhaExcel, false);

        List<ItemImportado> itens = new ArrayList<>();
        for (Row linha : linhas) {
            itens.add(montarItem(linha, colunas));
        }

        TaxasShopee taxas = null;
        boolean temColunaDeTaxa = colunas.containsKey(ColunaPlanilha.TAXA_COMISSAO)
                || colunas.containsKey(ColunaPlanilha.TAXA_SERVICO)
                || colunas.containsKey(ColunaPlanilha.TAXA_TRANSACAO);
        if (temColunaDeTaxa) {
            taxas = new TaxasShopee(
                    dinheiro(primeira, colunas.get(ColunaPlanilha.TAXA_COMISSAO), linhaExcel),
                    dinheiro(primeira, colunas.get(ColunaPlanilha.TAXA_SERVICO), linhaExcel),
                    dinheiro(primeira, colunas.get(ColunaPlanilha.TAXA_TRANSACAO), linhaExcel));
        }
        BigDecimal cupom = dinheiro(primeira, colunas.get(ColunaPlanilha.CUPOM_VENDEDOR), linhaExcel);

        return new PedidoImportado(codigo, status, criadoEm, prazoEnvio, cupom, taxas, List.copyOf(itens));
    }

    private ItemImportado montarItem(Row linha, Map<ColunaPlanilha, Integer> colunas) {
        int linhaExcel = linha.getRowNum() + 1;
        String nome = texto(linha, colunas.get(ColunaPlanilha.NOME_PRODUTO));
        String variacao = texto(linha, colunas.get(ColunaPlanilha.NOME_VARIACAO));
        if (nome.isBlank()) {
            throw new ValorInvalidoException(linhaExcel, "Linha sem nome do produto");
        }
        String sku = IdentificadorDeProduto.escolher(texto(linha, colunas.get(ColunaPlanilha.SKU_VARIACAO)),
                texto(linha, colunas.get(ColunaPlanilha.SKU_PRINCIPAL)), nome, variacao);

        BigDecimal quantidade = numero(linha, colunas.get(ColunaPlanilha.QUANTIDADE), linhaExcel);
        if (quantidade == null || quantidade.signum() <= 0 || quantidade.stripTrailingZeros().scale() > 0) {
            throw new ValorInvalidoException(linhaExcel, "Quantidade inválida");
        }
        BigDecimal preco = numero(linha, colunas.get(ColunaPlanilha.PRECO_ACORDADO), linhaExcel);
        if (preco == null || preco.signum() == 0) {
            BigDecimal original = numero(linha, colunas.get(ColunaPlanilha.PRECO_ORIGINAL), linhaExcel);
            preco = original != null ? original : preco;
        }
        if (preco == null || preco.signum() < 0) {
            throw new ValorInvalidoException(linhaExcel, "Preço do produto inválido");
        }
        return new ItemImportado(sku, IdentificadorDeProduto.descricao(nome, variacao), quantidade.intValueExact(),
                Moeda.arredondar(preco));
    }

    private static String texto(Row linha, Integer indice) {
        if (indice == null) {
            return "";
        }
        Cell celula = linha.getCell(indice);
        if (celula == null) {
            return "";
        }
        CellType tipo = celula.getCellType() == CellType.FORMULA ? celula.getCachedFormulaResultType() : celula.getCellType();
        return switch (tipo) {
            case STRING -> celula.getStringCellValue().trim();
            case NUMERIC -> BigDecimal.valueOf(celula.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case BOOLEAN -> String.valueOf(celula.getBooleanCellValue());
            default -> "";
        };
    }

    /** Aceita célula numérica ou texto nos formatos "R$ 1.234,56", "1234,56" e "1234.56". */
    private static BigDecimal numero(Row linha, Integer indice, int linhaExcel) {
        if (indice == null) {
            return null;
        }
        Cell celula = linha.getCell(indice);
        if (celula == null) {
            return null;
        }
        CellType tipo = celula.getCellType() == CellType.FORMULA ? celula.getCachedFormulaResultType() : celula.getCellType();
        if (tipo == CellType.NUMERIC) {
            return BigDecimal.valueOf(celula.getNumericCellValue()).setScale(4, RoundingMode.HALF_UP);
        }
        if (tipo != CellType.STRING || celula.getStringCellValue().isBlank()) {
            return null;
        }
        String valor = celula.getStringCellValue().replace("R$", "").replace(" ", "").replace(" ", "").trim();
        if (valor.contains(",")) {
            valor = valor.replace(".", "").replace(",", ".");
        }
        try {
            return new BigDecimal(valor);
        } catch (NumberFormatException e) {
            throw new ValorInvalidoException(linhaExcel, "Valor numérico inválido: \"" + celula.getStringCellValue() + "\"");
        }
    }

    private static BigDecimal dinheiro(Row linha, Integer indice, int linhaExcel) {
        BigDecimal valor = numero(linha, indice, linhaExcel);
        return valor == null ? Moeda.ZERO : Moeda.arredondar(valor.abs());
    }

    private Instant dataHora(Row linha, Integer indice, int linhaExcel, boolean obrigatoria) {
        Cell celula = indice == null ? null : linha.getCell(indice);
        if (celula != null && celula.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(celula)) {
            return celula.getLocalDateTimeCellValue().atZone(zona).toInstant();
        }
        String valor = texto(linha, indice);
        if (valor.isBlank()) {
            if (obrigatoria) {
                throw new ValorInvalidoException(linhaExcel, "Data de criação do pedido vazia");
            }
            return null;
        }
        for (DateTimeFormatter formato : FORMATOS_DATA_HORA) {
            try {
                return LocalDateTime.parse(valor, formato).atZone(zona).toInstant();
            } catch (DateTimeParseException ignorado) {
                // tenta o próximo formato
            }
        }
        for (DateTimeFormatter formato : FORMATOS_DATA) {
            try {
                return LocalDate.parse(valor, formato).atStartOfDay(zona).toInstant();
            } catch (DateTimeParseException ignorado) {
                // tenta o próximo formato
            }
        }
        throw new ValorInvalidoException(linhaExcel, "Data inválida: \"" + valor + "\"");
    }

    static class ValorInvalidoException extends RuntimeException {
        private final int linhaNoExcel;

        ValorInvalidoException(int linhaNoExcel, String mensagem) {
            super(mensagem);
            this.linhaNoExcel = linhaNoExcel;
        }
    }
}
