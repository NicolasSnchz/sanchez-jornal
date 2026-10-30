package com.jornal.support;

import java.time.Clock;
import java.time.ZoneId;

public final class Clocks {

    public static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private Clocks() {
    }

    /** Reloj detenido en TestData.NOW (jueves 29 de octubre de 2026, 10:00 a. m.). */
    public static Clock fixedAtTestNow() {
        return Clock.fixed(TestData.NOW.atZone(BOGOTA).toInstant(), BOGOTA);
    }
}
