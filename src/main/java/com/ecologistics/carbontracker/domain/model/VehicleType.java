package com.ecologistics.carbontracker.domain.model;

import java.math.BigDecimal;

/**
 * Tipos de vehículo soportados por el cálculo de huella de carbono,
 * cada uno con su factor de emisión base en kg CO2 por tonelada-kilómetro.
 */
public enum VehicleType {

    ELECTRICO(new BigDecimal("0.02")),
    HIBRIDO(new BigDecimal("0.08")),
    DIESEL(new BigDecimal("0.15"));

    private final BigDecimal emissionFactorKgPerTonneKm;

    VehicleType(BigDecimal emissionFactorKgPerTonneKm) {
        this.emissionFactorKgPerTonneKm = emissionFactorKgPerTonneKm;
    }

    public BigDecimal getEmissionFactorKgPerTonneKm() {
        return emissionFactorKgPerTonneKm;
    }
}
