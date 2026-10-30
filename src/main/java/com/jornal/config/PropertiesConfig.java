package com.jornal.config;

import com.jornal.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Propiedades tipadas y validadas al arrancar: si falta una, la aplicacion no inicia. */
@Configuration
@EnableConfigurationProperties({SecurityProperties.class, LaborProperties.class})
public class PropertiesConfig {
}
