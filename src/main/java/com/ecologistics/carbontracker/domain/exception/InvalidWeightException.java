package com.ecologistics.carbontracker.domain.exception;

/**
 * El peso de la carga es nulo o negativo.
 */
public class InvalidWeightException extends CarbonCalculationException {

    public InvalidWeightException(String message) {
        super(message);
    }
}
