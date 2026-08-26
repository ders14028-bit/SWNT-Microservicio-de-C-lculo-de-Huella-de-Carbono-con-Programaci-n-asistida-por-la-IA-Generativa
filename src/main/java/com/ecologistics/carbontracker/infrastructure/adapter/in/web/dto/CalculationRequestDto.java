package com.ecologistics.carbontracker.infrastructure.adapter.in.web.dto;

import com.ecologistics.carbontracker.domain.model.VehicleType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Payload de entrada del endpoint de cálculo. Las restricciones de Bean Validation
 * son una primera línea de defensa a nivel de transporte (errores 400 tempranos
 * y legibles); la validación exhaustiva de reglas de negocio sigue viviendo en
 * el dominio, que se aplica también a cualquier otro adaptador de entrada futuro.
 */
public record CalculationRequestDto(

        @NotNull(message = "El tipo de vehículo es obligatorio.")
        VehicleType vehicleType,

        @NotNull(message = "El peso de la carga es obligatorio.")
        @DecimalMin(value = "0.0", inclusive = true, message = "El peso de la carga no puede ser negativo.")
        @Digits(integer = 9, fraction = 3, message = "El peso admite máximo 9 dígitos enteros y 3 decimales.")
        BigDecimal weightTonnes,

        @NotNull(message = "La distancia es obligatoria.")
        @DecimalMin(value = "0.0", inclusive = false, message = "La distancia debe ser mayor que cero.")
        @Digits(integer = 9, fraction = 3, message = "La distancia admite máximo 9 dígitos enteros y 3 decimales.")
        BigDecimal distanceKm,

        @NotNull(message = "El factor de eficiencia es obligatorio.")
        @DecimalMin(value = "0.1", inclusive = true, message = "El factor de eficiencia debe ser mayor o igual a 0.1.")
        @DecimalMax(value = "2.0", inclusive = true, message = "El factor de eficiencia debe ser menor o igual a 2.0.")
        BigDecimal efficiencyFactor
) {
}
