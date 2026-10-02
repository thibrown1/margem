package io.github.thibrown1.margem.configuracao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

/** Tudo o que é configurável no sistema, em application.yml ou por variável de ambiente. */
@Validated
@ConfigurationProperties(prefix = "margem")
public record MargemProperties(
        @NotBlank String fusoHorario,
        @Valid @NotNull Loja loja,
        @Valid @NotNull TabelaTaxas tabelaTaxas,
        @Valid @NotNull Alertas alertas,
        @Valid @NotNull Telegram telegram,
        @Valid @NotNull Shopee shopee,
        @Valid @NotNull Seguranca seguranca) {

    public record Loja(
            @NotNull @PositiveOrZero BigDecimal custoEmbalagemPorPedido,
            @NotNull @PositiveOrZero BigDecimal aliquotaImposto) {
    }

    public record TabelaTaxas(
            String vigencia,
            @NotNull @PositiveOrZero BigDecimal taxaAdicionalPorItem,
            @NotEmpty List<@Valid Faixa> faixas) {

        public record Faixa(
                @NotNull @PositiveOrZero BigDecimal precoMinimo,
                @NotNull @PositiveOrZero BigDecimal comissaoPercentual,
                @NotNull @PositiveOrZero BigDecimal taxaFixa) {
        }
    }

    public record Alertas(
            boolean habilitado,
            @NotBlank String cron,
            @Positive int horasAntesDoPrazo,
            @Positive int reenviarAposHoras,
            @Positive int diasVerificarPrejuizo) {
    }

    public record Telegram(String botToken, String chatId, @NotBlank String urlBase) {

        public boolean configurado() {
            return botToken != null && !botToken.isBlank() && chatId != null && !chatId.isBlank();
        }
    }

    public record Shopee(
            @NotNull Modo modo,
            @Valid @NotNull Sincronizacao sincronizacao,
            @Valid @NotNull Api api) {

        public enum Modo {
            SIMULADA,
            API
        }

        public record Sincronizacao(boolean habilitada, @NotBlank String cron, @Positive int dias) {
        }

        public record Api(
                @NotBlank String host,
                Long partnerId,
                String partnerKey,
                Long shopId,
                String accessToken,
                String refreshToken) {
        }
    }

    public record Seguranca(
            String jwtSegredo,
            @Positive int jwtValidadeMinutos,
            String senhaInicialDono,
            String senhaInicialExpedicao) {
    }
}
