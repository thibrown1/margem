package io.github.thibrown1.margem.adaptador.saida.shopee;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * Assinatura exigida pela Shopee Open Platform v2: HMAC-SHA256 em hexadecimal minúsculo,
 * usando a partner_key como chave. A string base muda conforme o tipo de chamada:
 * <ul>
 *     <li>chamadas de loja: partner_id + path + timestamp + access_token + shop_id</li>
 *     <li>chamadas públicas (como renovar o token): partner_id + path + timestamp</li>
 * </ul>
 */
public final class AssinaturaShopee {

    private AssinaturaShopee() {
    }

    public static String assinar(String partnerKey, String textoBase) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(partnerKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(hmac.doFinal(textoBase.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível assinar a requisição para a Shopee", e);
        }
    }

    public static String textoBaseDaLoja(long partnerId, String path, long timestamp, String accessToken, long shopId) {
        return partnerId + path + timestamp + accessToken + shopId;
    }

    public static String textoBasePublico(long partnerId, String path, long timestamp) {
        return partnerId + path + timestamp;
    }
}
