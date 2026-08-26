package com.ecologistics.carbontracker.domain.exception;

/**
 * El factor de eficiencia energética es nulo o está fuera del rango válido.
 */
public class InvalidEfficiencyFactorException extends CarbonCalculationException {

    public InvalidEfficiencyFactorException(String message) {
        super(message);
    }
}
