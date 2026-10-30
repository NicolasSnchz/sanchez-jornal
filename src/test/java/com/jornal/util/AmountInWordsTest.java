package com.jornal.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** El valor en letras del comprobante sigue las reglas del espanol, incluidos los casos dificiles. */
class AmountInWordsTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
        "0|cero pesos",
        "1|un peso",
        "16|diecis\u00e9is pesos",
        "21|veinti\u00fan pesos",
        "31|treinta y un pesos",
        "100|cien pesos",
        "101|ciento un pesos",
        "500|quinientos pesos",
        "1000|mil pesos",
        "1100|mil cien pesos",
        "21000|veinti\u00fan mil pesos",
        "66702|sesenta y seis mil setecientos dos pesos",
        "75000|setenta y cinco mil pesos",
        "98438|noventa y ocho mil cuatrocientos treinta y ocho pesos",
        "100000|cien mil pesos",
        "101000|ciento un mil pesos",
        "437500|cuatrocientos treinta y siete mil quinientos pesos",
        "1000000|un mill\u00f3n de pesos",
        "1000001|un mill\u00f3n un pesos",
        "2500000|dos millones quinientos mil pesos",
        "21000000|veinti\u00fan millones de pesos",
        "1000000000|mil millones de pesos"
    })
    void pesos_escribeElValorComoEnUnComprobante(long value, String expected) {
        assertThat(AmountInWords.pesos(BigDecimal.valueOf(value))).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 1_000_000_000_000L})
    void pesos_fueraDeRango_esRechazado(long value) {
        assertThatThrownBy(() -> AmountInWords.pesos(BigDecimal.valueOf(value)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
