package com.jornal.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Reloj que la prueba puede adelantar: simula dias de trabajo sin esperar. */
public class MutableClock extends Clock {

    private Instant instant;
    private final ZoneId zone;

    public MutableClock(LocalDateTime start, ZoneId zone) {
        this.instant = start.atZone(zone).toInstant();
        this.zone = zone;
    }

    public void setTo(LocalDateTime dateTime) {
        this.instant = dateTime.atZone(zone).toInstant();
    }

    public void advance(Duration duration) {
        this.instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(LocalDateTime.ofInstant(instant, this.zone), zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
