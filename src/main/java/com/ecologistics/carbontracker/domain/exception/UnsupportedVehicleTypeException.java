package com.ecologistics.carbontracker.domain.exception;

/**
 * El tipo de vehículo es nulo o no está soportado por el dominio.
 */
public class UnsupportedVehicleTypeException extends CarbonCalculationException {

    public UnsupportedVehicleTypeException(String message) {
        super(message);
    }
}
