package io.github.thibrown1.margem.aplicacao.alerta;

import java.time.Duration;

/**
 * @param antecedenciaDoPrazo  avisa pedidos cujo prazo de envio vence dentro deste intervalo (ou já venceu)
 * @param reenviarApos         o mesmo alerta só é repetido depois deste intervalo
 * @param janelaDePrejuizo     olha pedidos criados neste intervalo para achar os que deram prejuízo
 */
public record ParametrosAlertas(Duration antecedenciaDoPrazo, Duration reenviarApos, Duration janelaDePrejuizo) {
}
