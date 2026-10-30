package com.jornal.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba de integracion: aplicacion completa (seguridad, servicios, JPA y
 * Flyway sobre H2) con un reloj controlable. Cada prueba corre en una
 * transaccion que se revierte al terminar, asi ninguna deja datos para otra.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestBeans.class)
@Transactional
public @interface IntegrationTest {
}
