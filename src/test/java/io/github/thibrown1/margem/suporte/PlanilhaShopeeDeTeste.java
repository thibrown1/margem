package io.github.thibrown1.margem.suporte;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Monta, em memória, uma planilha no formato da exportação de pedidos da Central do Vendedor. */
public class PlanilhaShopeeDeTeste {

    public static final List<String> CABECALHO_PADRAO = List.of(
            "ID do pedido", "Status do pedido", "Data de criação do pedido", "Data prevista de envio",
            "Nome do Produto", "Número de referência SKU", "SKU principal", "Nome da variação",
            "Preço original", "Preço acordado", "Quantidade", "Cupom do vendedor",
            "Taxa de comissão", "Taxa de serviço", "Taxa de transação",
            "Nome do destinatário", "Telefone", "Endereço de entrega");

    private final List<String> cabecalho;
    private final List<List<Object>> linhas = new ArrayList<>();

    private PlanilhaShopeeDeTeste(List<String> cabecalho) {
        this.cabecalho = cabecalho;
    }

    public static PlanilhaShopeeDeTeste padrao() {
        return new PlanilhaShopeeDeTeste(CABECALHO_PADRAO);
    }

    public static PlanilhaShopeeDeTeste comCabecalho(String... colunas) {
        return new PlanilhaShopeeDeTeste(List.of(colunas));
    }

    /** Valores na ordem do cabeçalho. Use String, Number ou null. */
    public PlanilhaShopeeDeTeste linha(Object... valores) {
        linhas.add(Arrays.asList(valores));
        return this;
    }

    public byte[] bytes() {
        try (XSSFWorkbook planilha = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            Sheet aba = planilha.createSheet("orders");
            Row linhaCabecalho = aba.createRow(0);
            for (int i = 0; i < cabecalho.size(); i++) {
                linhaCabecalho.createCell(i).setCellValue(cabecalho.get(i));
            }
            for (int l = 0; l < linhas.size(); l++) {
                Row linha = aba.createRow(l + 1);
                List<Object> valores = linhas.get(l);
                for (int c = 0; c < valores.size(); c++) {
                    Object valor = valores.get(c);
                    if (valor instanceof Number numero) {
                        linha.createCell(c).setCellValue(numero.doubleValue());
                    } else if (valor != null) {
                        linha.createCell(c).setCellValue(valor.toString());
                    }
                }
            }
            planilha.write(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
