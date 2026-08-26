package com.ecologistics.carbontracker.domain;

import com.ecologistics.carbontracker.domain.exception.InvalidDistanceException;
import com.ecologistics.carbontracker.domain.exception.InvalidEfficiencyFactorException;
import com.ecologistics.carbontracker.domain.exception.InvalidWeightException;
import com.ecologistics.carbontracker.domain.exception.UnsupportedVehicleTypeException;
import com.ecologistics.carbontracker.domain.model.CalculationRequest;
import com.ecologistics.carbontracker.domain.model.VehicleType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

/**
 * Calcula la huella de carbono (kg CO2) de un envío según:
 * emissionFactor(vehicleType) x peso(t) x distancia(km) x factor de eficiencia.
 * Clase de dominio pura: sin dependencias de Spring, testeable en aislamiento.
 */
public class CarbonFootprintCalculator {

    private static final int RESULT_SCALE = 4;
    private static final BigDecimal MIN_EFFICIENCY_FACTOR = new BigDecimal("0.1");
    private static final BigDecimal MAX_EFFICIENCY_FACTOR = new BigDecimal("2.0");

    public BigDecimal calculate(CalculationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de cálculo no puede ser nula.");
        }

        validateVehicleType(request.vehicleType());
        validateDistance(request.distanceKm());
        validateWeight(request.weightTonnes());
        validateEfficiencyFactor(request.efficiencyFactor());

        return request.vehicleType().getEmissionFactorKgPerTonneKm()
                .multiply(request.weightTonnes())
                .multiply(request.distanceKm())
                .multiply(request.efficiencyFactor())
                .setScale(RESULT_SCALE, RoundingMode.HALF_UP);
    }

    private void validateVehicleType(VehicleType vehicleType) {
        if (vehicleType == null) {
            throw new UnsupportedVehicleTypeException(
                    "El tipo de vehículo es obligatorio y debe ser uno de: " + Arrays.toString(VehicleType.values()));
        }
    }

    private void validateDistance(BigDecimal distanceKm) {
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidDistanceException(
                    "La distancia debe ser mayor que cero. Valor recibido: " + distanceKm);
        }
    }

    private void validateWeight(BigDecimal weightTonnes) {
        if (weightTonnes == null || weightTonnes.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidWeightException(
                    "El peso de la carga no puede ser negativo. Valor recibido: " + weightTonnes);
        }
    }

    private void validateEfficiencyFactor(BigDecimal efficiencyFactor) {
        if (efficiencyFactor == null
                || efficiencyFactor.compareTo(MIN_EFFICIENCY_FACTOR) < 0
                || efficiencyFactor.compareTo(MAX_EFFICIENCY_FACTOR) > 0) {
            throw new InvalidEfficiencyFactorException(
                    "El factor de eficiencia debe estar entre " + MIN_EFFICIENCY_FACTOR
                            + " y " + MAX_EFFICIENCY_FACTOR + ". Valor recibido: " + efficiencyFactor);
        }
    }
}
