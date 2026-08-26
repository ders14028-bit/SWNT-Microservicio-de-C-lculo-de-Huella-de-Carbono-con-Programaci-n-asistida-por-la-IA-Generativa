package com.ecologistics.carbontracker.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;

/**
 * Payload de salida del endpoint de cálculo.
 */
public record CalculationResponseDto(
        BigDecimal carbonFootprintKg
) {
}
