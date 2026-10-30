package com.jornal.entity;

/** Clase de pago; cada una tiene su propia estrategia de registro. */
public enum PaymentType {
    ADVANCE, SETTLEMENT, FINAL_SETTLEMENT
}
