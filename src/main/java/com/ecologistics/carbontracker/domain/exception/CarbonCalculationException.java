package com.ecologistics.carbontracker.domain.exception;

/**
 * Raíz común de las excepciones de dominio lanzadas al validar una
 * {@link com.ecologistics.carbontracker.domain.model.CalculationRequest}.
 * Permite capturarlas de forma polimórfica (p. ej. en un @ControllerAdvice)
 * sin perder el detalle de la causa específica.
 */
public abstract class CarbonCalculationException extends RuntimeException {

    protected CarbonCalculationException(String message) {
        super(message);
    }
}
