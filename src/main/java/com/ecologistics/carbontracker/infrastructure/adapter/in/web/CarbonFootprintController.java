package com.ecologistics.carbontracker.infrastructure.adapter.in.web;

import com.ecologistics.carbontracker.application.port.in.CalculateCarbonFootprintUseCase;
import com.ecologistics.carbontracker.infrastructure.adapter.in.web.dto.CalculationRequestDto;
import com.ecologistics.carbontracker.infrastructure.adapter.in.web.dto.CalculationResponseDto;
import com.ecologistics.carbontracker.infrastructure.adapter.in.web.mapper.CalculationRequestMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Adaptador de entrada (driving adapter) REST. Traduce peticiones HTTP al
 * caso de uso de la capa de aplicación; no contiene lógica de negocio.
 */
@RestController
@RequestMapping("/api/v1/carbon-footprint")
public class CarbonFootprintController {

    private final CalculateCarbonFootprintUseCase calculateCarbonFootprintUseCase;
    private final CalculationRequestMapper mapper;

    public CarbonFootprintController(CalculateCarbonFootprintUseCase calculateCarbonFootprintUseCase,
                                      CalculationRequestMapper mapper) {
        this.calculateCarbonFootprintUseCase = calculateCarbonFootprintUseCase;
        this.mapper = mapper;
    }

    @PostMapping("/calculate")
    public ResponseEntity<CalculationResponseDto> calculate(@Valid @RequestBody CalculationRequestDto requestDto) {
        BigDecimal carbonFootprintKg = calculateCarbonFootprintUseCase.calculate(mapper.toDomain(requestDto));
        return ResponseEntity.status(HttpStatus.OK).body(mapper.toResponse(carbonFootprintKg));
    }
}
