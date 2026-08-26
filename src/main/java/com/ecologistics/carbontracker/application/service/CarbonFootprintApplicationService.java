package com.ecologistics.carbontracker.application.service;

import com.ecologistics.carbontracker.application.port.in.CalculateCarbonFootprintUseCase;
import com.ecologistics.carbontracker.domain.CarbonFootprintCalculator;
import com.ecologistics.carbontracker.domain.model.CalculationRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Orquesta el caso de uso de cálculo de huella de carbono.
 * No contiene reglas de negocio propias: delega la fórmula y las validaciones
 * al dominio ({@link CarbonFootprintCalculator}). Es el único punto que conoce
 * tanto el puerto de entrada como el dominio.
 */
@Service
public class CarbonFootprintApplicationService implements CalculateCarbonFootprintUseCase {

    private final CarbonFootprintCalculator calculator;

    public CarbonFootprintApplicationService(CarbonFootprintCalculator calculator) {
        this.calculator = calculator;
    }

    @Override
    public BigDecimal calculate(CalculationRequest request) {
        return calculator.calculate(request);
    }
}
