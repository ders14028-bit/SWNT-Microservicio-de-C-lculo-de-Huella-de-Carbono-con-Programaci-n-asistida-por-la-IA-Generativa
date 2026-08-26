package com.ecologistics.carbontracker.domain.model;

import java.math.BigDecimal;

/**
 * Datos de entrada para el cálculo de huella de carbono de un envío.
 * Contrato puro de dominio: no depende de frameworks ni de anotaciones de validación web.
 */
public record CalculationRequest(
        VehicleType vehicleType,
        BigDecimal weightTonnes,
        BigDecimal distanceKm,
        BigDecimal efficiencyFactor
) {
}
