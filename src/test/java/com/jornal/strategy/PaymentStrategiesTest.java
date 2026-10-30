package com.jornal.strategy;

import static com.jornal.support.TestData.confirmedDay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jornal.entity.Assignment;
import com.jornal.entity.Payment;
import com.jornal.entity.PaymentMethod;
import com.jornal.entity.PaymentType;
import com.jornal.entity.Settlement;
import com.jornal.exception.BusinessRuleException;
import com.jornal.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Las tres estrategias de pago sobre una misma asignacion de $80.000 diarios.
 * Semana 1: lunes 19 a viernes 23 de octubre de 2026. Semana 2: desde el lunes 26.
 * (El lunes 12 de octubre es festivo, Dia de la Raza, por eso no se usa).
 */
class PaymentStrategiesTest {

    private static final LocalDate W1_MON = LocalDate.of(2026, 10, 19);
    private static final LocalDate W1_FRI = W1_MON.plusDays(4);
    private static final LocalDate W2_MON = LocalDate.of(2026, 10, 26);
    private static final LocalDate W2_FRI = W2_MON.plusDays(4);
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 31);

    private final AdvancePaymentStrategy advance = new AdvancePaymentStrategy();
    private final PeriodSettlementStrategy settlement = new PeriodSettlementStrategy();
    private final FinalSettlementStrategy finalSettlement = new FinalSettlementStrategy();
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        assignment = TestData.assignmentInProgress();
        for (LocalDate d = W1_MON; !d.isAfter(W1_FRI); d = d.plusDays(1)) {
            confirmedDay(assignment, d);
        }
    }

    @Nested
    class Anticipo {

        @Test
        void quote_hastaElSaldoConfirmado_esValido() {
            PaymentQuote quote = advance.quote(assignment, advanceOf("400000"), TODAY);

            assertThat(quote.amount()).isEqualByComparingTo("400000");
        }

        @Test
        void quote_masQueElSaldo_esRechazadoYMuestraElSaldo() {
            assertThatThrownBy(() -> advance.quote(assignment, advanceOf("400001"), TODAY))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(e -> {
                        BusinessRuleException rule = (BusinessRuleException) e;
                        assertThat(rule.getCode()).isEqualTo("pago.anticipo.excede.saldo");
                        assertThat(rule.getArgs()).containsExactly(new BigDecimal("400000"));
                    });
        }

        @Test
        void quote_sinMotivo_esRechazado() {
            PaymentCommand command = new PaymentCommand(PaymentType.ADVANCE, new BigDecimal("10000"),
                    PaymentMethod.CASH, null, " ", false, null);

            assertThatThrownBy(() -> advance.quote(assignment, command, TODAY))
                    .extracting("code").isEqualTo("pago.anticipo.motivo.requerido");
        }

        @Test
        void quote_jornadasSinConfirmar_noCuentanParaElTope() {
            assignment.registerWorkDay(W2_MON, TestData.fullDay(), TODAY, TODAY.atTime(8, 0));

            assertThatThrownBy(() -> advance.quote(assignment, advanceOf("480000"), TODAY))
                    .extracting("code").isEqualTo("pago.anticipo.excede.saldo");
        }
    }

    @Nested
    class LiquidacionDePeriodo {

        @Test
        void quote_descuentaLosAnticiposDelPeriodo() {
            advance.create(assignment, advanceOf("100000"), "JOR-000001", W1_FRI.atTime(12, 0));

            PaymentQuote quote = settlement.quote(assignment, settlementUntil(W1_FRI), TODAY);

            assertThat(quote.periodStart()).isEqualTo(LocalDate.of(2026, 10, 5));
            assertThat(quote.periodEnd()).isEqualTo(W1_FRI);
            assertThat(quote.earnedInPeriod()).isEqualByComparingTo("400000");
            assertThat(quote.discounted()).isEqualByComparingTo("100000");
            assertThat(quote.amount()).isEqualByComparingTo("300000");
        }

        @Test
        void create_cierraElPeriodoYElSiguienteEmpiezaAlOtroDia() {
            settlement.create(assignment, settlementUntil(W1_FRI), "JOR-000002", W1_FRI.atTime(17, 0));

            assertThat(assignment.calculateBalance()).isZero();
            assertThat(assignment.nextUnsettledDate()).isEqualTo(W1_FRI.plusDays(1));
            // El viernes 9 no se trabajo, pero ya quedo dentro de un periodo liquidado.
            LocalDate insideSettledPeriod = LocalDate.of(2026, 10, 9);
            assertThatThrownBy(() -> assignment.registerWorkDay(insideSettledPeriod, TestData.fullDay(), TODAY,
                    TODAY.atTime(8, 0))).extracting("code").isEqualTo("jornada.periodo.liquidado");
        }

        @Test
        void quote_corteConJornadasSinConfirmar_esRechazado() {
            assignment.registerWorkDay(W2_MON, TestData.fullDay(), TODAY, TODAY.atTime(8, 0));

            assertThatThrownBy(() -> settlement.quote(assignment, settlementUntil(W2_FRI), TODAY))
                    .extracting("code").isEqualTo("pago.liquidacion.jornadas.sin.confirmar");
        }

        @Test
        void quote_corteFuturo_esRechazado() {
            assertThatThrownBy(() -> settlement.quote(assignment, settlementUntil(TODAY.plusDays(1)), TODAY))
                    .extracting("code").isEqualTo("pago.liquidacion.periodo.futuro");
        }

        @Test
        void quote_corteAntesDelPeriodoAbierto_esRechazado() {
            settlement.create(assignment, settlementUntil(W1_FRI), "JOR-000003", W1_FRI.atTime(17, 0));

            assertThatThrownBy(() -> settlement.quote(assignment, settlementUntil(W1_MON), TODAY))
                    .extracting("code").isEqualTo("pago.liquidacion.periodo.invalido");
        }

        @Test
        void quote_anticiposQueCubrenTodo_noDejaSaldoPorLiquidar() {
            advance.create(assignment, advanceOf("400000"), "JOR-000004", W1_FRI.atTime(12, 0));

            assertThatThrownBy(() -> settlement.quote(assignment, settlementUntil(W1_FRI), TODAY))
                    .extracting("code").isEqualTo("pago.liquidacion.sin.saldo");
        }
    }

    @Nested
    class LiquidacionFinal {

        @Test
        void create_pagaTodoElSaldoYCierraElRegistroDeJornadas() {
            Payment payment = finalSettlement.create(assignment, finalCommand(), "JOR-000005", TODAY.atTime(9, 0));

            assertThat(payment.getAmount()).isEqualByComparingTo("400000");
            assertThat(payment.getType()).isEqualTo(PaymentType.FINAL_SETTLEMENT);
            assertThat(((Settlement) payment).getPeriodEnd()).isEqualTo(W1_FRI);
            assertThat(assignment.calculateBalance()).isZero();
            assertThatThrownBy(() -> assignment.registerWorkDay(W2_MON, TestData.fullDay(), TODAY, TODAY.atTime(9, 0)))
                    .extracting("code").isEqualTo("jornada.asignacion.liquidada");
        }

        @Test
        void quote_conJornadasPorConfirmar_esRechazado() {
            assignment.registerWorkDay(W2_MON, TestData.fullDay(), TODAY, TODAY.atTime(8, 0));

            assertThatThrownBy(() -> finalSettlement.quote(assignment, finalCommand(), TODAY))
                    .extracting("code").isEqualTo("pago.final.jornadas.sin.confirmar");
        }
    }

    @Nested
    class Anulacion {

        @Test
        void cancel_liquidacionQueNoEsLaUltima_esRechazada() {
            Payment first = settlement.create(assignment, settlementUntil(W1_MON), "JOR-000006", W1_MON.atTime(17, 0));
            settlement.create(assignment, settlementUntil(W1_FRI), "JOR-000007", W1_FRI.atTime(17, 0));

            assertThat(first.canBeCancelled()).isFalse();
            assertThatThrownBy(() -> first.cancel("Error", TODAY.atTime(9, 0)))
                    .extracting("code").isEqualTo("pago.anular.liquidacion.no.ultima");
        }

        @Test
        void cancel_anticipoYaDescontadoEnUnaLiquidacion_esRechazado() {
            Payment advancePayment = advance.create(assignment, advanceOf("50000"), "JOR-000008", W1_MON.atTime(18, 0));
            settlement.create(assignment, settlementUntil(W1_FRI), "JOR-000009", W1_FRI.atTime(17, 0));

            assertThatThrownBy(() -> advancePayment.cancel("Error", TODAY.atTime(9, 0)))
                    .extracting("code").isEqualTo("pago.anular.anticipo.descontado");
        }

        @Test
        void cancel_pagoConfirmadoPorElTrabajador_esRechazado() {
            Payment payment = advance.create(assignment, advanceOf("50000"), "JOR-000010", TODAY.atTime(8, 0));
            payment.confirmReceipt(TODAY.atTime(9, 0));

            assertThatThrownBy(() -> payment.cancel("Error", TODAY.atTime(10, 0)))
                    .extracting("code").isEqualTo("pago.anular.estado.invalido");
        }

        @Test
        void cancel_ultimaLiquidacion_reabreElPeriodo() {
            Payment payment = settlement.create(assignment, settlementUntil(W1_FRI), "JOR-000011", W1_FRI.atTime(17, 0));

            payment.cancel("Se digito mal", TODAY.atTime(8, 0));

            assertThat(assignment.calculateBalance()).isEqualByComparingTo("400000");
            assertThat(assignment.nextUnsettledDate()).isEqualTo(LocalDate.of(2026, 10, 5));
        }
    }

    private static PaymentCommand advanceOf(String amount) {
        return new PaymentCommand(PaymentType.ADVANCE, new BigDecimal(amount), PaymentMethod.CASH, null,
                "Transporte de la semana", true, null);
    }

    private static PaymentCommand settlementUntil(LocalDate end) {
        return new PaymentCommand(PaymentType.SETTLEMENT, null, PaymentMethod.CASH, null, null, false, end);
    }

    private static PaymentCommand finalCommand() {
        return new PaymentCommand(PaymentType.FINAL_SETTLEMENT, null, PaymentMethod.BANK_TRANSFER, "BCO-1", null,
                false, null);
    }
}
