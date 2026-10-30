package com.jornal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Jornal: contratacion de ayudantes de obra por dias y control de cada jornada,
 * anticipo y liquidacion.
 */
@SpringBootApplication
public class JornalApplication {

    public static void main(String[] args) {
        SpringApplication.run(JornalApplication.class, args);
    }
}
