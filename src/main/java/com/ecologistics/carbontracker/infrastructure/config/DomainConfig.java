package com.ecologistics.carbontracker.infrastructure.config;

import com.ecologistics.carbontracker.domain.CarbonFootprintCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cablea como beans de Spring las clases de dominio, que deliberadamente
 * no llevan anotaciones de framework para mantenerse puras y testeables
 * sin contexto de Spring.
 */
@Configuration
public class DomainConfig {

    @Bean
    public CarbonFootprintCalculator carbonFootprintCalculator() {
        return new CarbonFootprintCalculator();
    }
}
