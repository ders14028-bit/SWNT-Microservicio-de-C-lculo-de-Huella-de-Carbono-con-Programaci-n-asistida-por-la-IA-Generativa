package com.ecologistics.carbontracker.domain.exception;

/**
 * La distancia del envío es nula, negativa o cero.
 */
public class InvalidDistanceException extends CarbonCalculationException {

    public InvalidDistanceException(String message) {
        super(message);
    }
}
