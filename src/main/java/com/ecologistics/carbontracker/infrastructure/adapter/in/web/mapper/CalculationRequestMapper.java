package com.ecologistics.carbontracker.infrastructure.adapter.in.web.mapper;

import com.ecologistics.carbontracker.domain.model.CalculationRequest;
import com.ecologistics.carbontracker.infrastructure.adapter.in.web.dto.CalculationRequestDto;
import com.ecologistics.carbontracker.infrastructure.adapter.in.web.dto.CalculationResponseDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Traduce entre los DTOs del adaptador web y el modelo del dominio,
 * evitando que el dominio conozca detalles de la capa de transporte.
 */
@Component
public class CalculationRequestMapper {

    public CalculationRequest toDomain(CalculationRequestDto dto) {
        return new CalculationRequest(
                dto.vehicleType(),
                dto.weightTonnes(),
                dto.distanceKm(),
                dto.efficiencyFactor()
        );
    }

    public CalculationResponseDto toResponse(BigDecimal carbonFootprintKg) {
        return new CalculationResponseDto(carbonFootprintKg);
    }
}
