package com.ecologistics.carbontracker.application.port.in;

import com.ecologistics.carbontracker.domain.model.CalculationRequest;

import java.math.BigDecimal;

/**
 * Puerto de entrada (driving port): contrato que expone el caso de uso de cálculo
 * de huella de carbono a cualquier adaptador de entrada (REST, mensajería, CLI, etc.),
 * sin acoplarlos a los detalles de orquestación ni al dominio interno.
 */
public interface CalculateCarbonFootprintUseCase {

    BigDecimal calculate(CalculationRequest request);
}
