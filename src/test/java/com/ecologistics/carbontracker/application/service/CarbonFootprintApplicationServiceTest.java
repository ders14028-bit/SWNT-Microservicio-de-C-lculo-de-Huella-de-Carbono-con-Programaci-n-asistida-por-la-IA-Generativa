package com.ecologistics.carbontracker.application.service;

import com.ecologistics.carbontracker.domain.CarbonFootprintCalculator;
import com.ecologistics.carbontracker.domain.exception.UnsupportedVehicleTypeException;
import com.ecologistics.carbontracker.domain.model.CalculationRequest;
import com.ecologistics.carbontracker.domain.model.VehicleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de {@link CarbonFootprintApplicationService}: un orquestador puro que
 * delega en {@link CarbonFootprintCalculator} sin transformar datos ni agregar
 * lógica propia. El calculador se mockea para aislar al service de la fórmula
 * y las reglas de negocio del dominio, que ya están cubiertas en su propia suite.
 */
@ExtendWith(MockitoExtension.class)
class CarbonFootprintApplicationServiceTest {

    @Mock
    private CarbonFootprintCalculator calculator;

    @InjectMocks
    private CarbonFootprintApplicationService service;

    @Test
    @DisplayName("calculate() devuelve exactamente el valor producido por el calculador")
    void returnsExactValueProducedByCalculator() {
        CalculationRequest request = new CalculationRequest(
                VehicleType.DIESEL, new BigDecimal("10"), new BigDecimal("100"), new BigDecimal("1.0"));
        BigDecimal expected = new BigDecimal("150.0000");
        when(calculator.calculate(request)).thenReturn(expected);

        BigDecimal result = service.calculate(request);

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("Delega en el calculador exactamente una vez, pasando el mismo objeto request sin modificarlo")
    void delegatesToCalculatorWithTheSameRequestInstance() {
        CalculationRequest request = new CalculationRequest(
                VehicleType.HIBRIDO, new BigDecimal("5"), new BigDecimal("200"), new BigDecimal("1.5"));
        when(calculator.calculate(any())).thenReturn(BigDecimal.TEN);

        service.calculate(request);

        verify(calculator, times(1)).calculate(request);
    }

    @Test
    @DisplayName("No captura ni transforma las excepciones de dominio: las propaga tal cual")
    void propagatesDomainExceptionsUnchanged() {
        CalculationRequest request = new CalculationRequest(
                null, new BigDecimal("10"), new BigDecimal("100"), new BigDecimal("1.0"));
        UnsupportedVehicleTypeException thrownByCalculator =
                new UnsupportedVehicleTypeException("Tipo de vehículo no soportado.");
        when(calculator.calculate(request)).thenThrow(thrownByCalculator);

        UnsupportedVehicleTypeException propagated = assertThrows(
                UnsupportedVehicleTypeException.class,
                () -> service.calculate(request)
        );

        assertSame(thrownByCalculator, propagated);
    }
}
