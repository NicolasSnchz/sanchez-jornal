package com.jornal.support;

import com.jornal.repository.AssignmentRepository;
import com.jornal.repository.ConstructionSiteRepository;
import com.jornal.repository.UserRepository;
import java.time.Clock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
public class TestBeans {

    /** Reemplaza el reloj del sistema: "hoy" es el jueves 29 de octubre de 2026 a las 10:00 a. m. */
    @Bean
    @Primary
    public MutableClock testClock() {
        return new MutableClock(TestData.NOW, Clocks.BOGOTA);
    }

    @Bean
    public Scenario scenario(UserRepository users, ConstructionSiteRepository sites,
                             AssignmentRepository assignments, PasswordEncoder encoder, Clock clock) {
        return new Scenario(users, sites, assignments, encoder, clock);
    }
}
