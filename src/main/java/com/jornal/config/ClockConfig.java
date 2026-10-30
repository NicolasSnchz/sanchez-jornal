package com.jornal.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj de la aplicacion. Ningun servicio llama a LocalDate.now() directamente:
 * todos reciben este Clock, asi las pruebas pueden fijar "hoy" y verificar
 * reglas como "no se registran jornadas futuras" sin depender del calendario real.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.timezone:America/Bogota}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
