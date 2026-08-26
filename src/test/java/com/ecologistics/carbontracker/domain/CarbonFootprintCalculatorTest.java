package com.ecologistics.carbontracker.domain;

import com.ecologistics.carbontracker.domain.exception.InvalidDistanceException;
import com.ecologistics.carbontracker.domain.exception.InvalidEfficiencyFactorException;
import com.ecologistics.carbontracker.domain.exception.InvalidWeightException;
import com.ecologistics.carbontracker.domain.exception.UnsupportedVehicleTypeException;
import com.ecologistics.carbontracker.domain.model.CalculationRequest;
import com.ecologistics.carbontracker.domain.model.VehicleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas de la clase de dominio pura {@link CarbonFootprintCalculator}.
 * No usa Mockito: el calculador no tiene colaboradores que mockear.
 */
class CarbonFootprintCalculatorTest {

    private final CarbonFootprintCalculator calculator = new CarbonFootprintCalculator();

    @DisplayName("Calcula correctamente las emisiones para cada tipo de vehículo")
    @ParameterizedTest(name = "{0}: peso={1}t, distancia={2}km, eficiencia={3} -> {4} kg CO2")
    @MethodSource("happyPathScenarios")
    void calculatesEmissionsForEachVehicleType(VehicleType vehicleType, BigDecimal weight,
                                                BigDecimal distance, BigDecimal efficiencyFactor,
                                                BigDecimal expected) {
        CalculationRequest request = new CalculationRequest(vehicleType, weight, distance, efficiencyFactor);

        BigDecimal result = calculator.calculate(request);

        assertThat(result).isEqualByComparingTo(expected);
    }

    private static Stream<Arguments> happyPathScenarios() {
        return Stream.of(
                // emissionFactor x peso x distancia x eficiencia
                Arguments.of(VehicleType.ELECTRICO, new BigDecimal("10"), new BigDecimal("100"),
                        new BigDecimal("1.0"), new BigDecimal("20.0000")),        // 0.02 x 10 x 100 x 1.0
                Arguments.of(VehicleType.HIBRIDO, new BigDecimal("5"), new BigDecimal("200"),
                        new BigDecimal("1.5"), new BigDecimal("120.0000")),       // 0.08 x 5 x 200 x 1.5
                Arguments.of(VehicleType.DIESEL, new BigDecimal("8"), new BigDecimal("50"),
                        new BigDecimal("1.2"), new BigDecimal("72.0000"))         // 0.15 x 8 x 50 x 1.2
        );
    }

    @Test
    @DisplayName("Peso en cero es válido (no es negativo) y produce cero emisiones")
    void zeroWeightProducesZeroEmissions() {
        CalculationRequest request = new CalculationRequest(
                VehicleType.DIESEL, BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("1.0"));

        assertThat(calculator.calculate(request)).isEqualByComparingTo("0.0000");
    }

    @ParameterizedTest(name = "distancia={0} debe lanzar InvalidDistanceException")
    @CsvSource({"0", "-1", "-100.5"})
    void rejectsZeroOrNegativeDistance(BigDecimal distance) {
        CalculationRequest request = new CalculationRequest(
                VehicleType.DIESEL, new BigDecimal("10"), distance, new BigDecimal("1.0"));

        assertThrows(InvalidDistanceException.class, () -> calculator.calculate(request));
    }

    @Test
    @DisplayName("Distancia nula lanza InvalidDistanceException")
    void rejectsNullDistance() {
        CalculationRequest request = new CalculationRequest(
                VehicleType.DIESEL, new BigDecimal("10"), null, new BigDecimal("1.0"));

        assertThrows(InvalidDistanceException.class, () -> calculator.calculate(request));
    }

    @ParameterizedTest(name = "peso={0} debe lanzar InvalidWeightException")
    @CsvSource({"-1", "-0.01", "-1000"})
    void rejectsNegativeWeight(BigDecimal weight) {
        CalculationRequest request = new CalculationRequest(
                VehicleType.ELECTRICO, weight, new BigDecimal("100"), new BigDecimal("1.0"));

        assertThrows(InvalidWeightException.class, () -> calculator.calculate(request));
    }

    @Test
    @DisplayName("Peso nulo lanza InvalidWeightException")
    void rejectsNullWeight() {
        CalculationRequest request = new CalculationRequest(
                VehicleType.ELECTRICO, null, new BigDecimal("100"), new BigDecimal("1.0"));

        assertThrows(InvalidWeightException.class, () -> calculator.calculate(request));
    }

    @Test
    @DisplayName("Tipo de vehículo nulo (no soportado) lanza UnsupportedVehicleTypeException")
    void rejectsNullVehicleType() {
        CalculationRequest request = new CalculationRequest(
                null, new BigDecimal("10"), new BigDecimal("100"), new BigDecimal("1.0"));

        assertThrows(UnsupportedVehicleTypeException.class, () -> calculator.calculate(request));
    }

    @Test
    @DisplayName("Factor de eficiencia nulo lanza InvalidEfficiencyFactorException")
    void rejectsNullEfficiencyFactor() {
        CalculationRequest request = new CalculationRequest(
                VehicleType.HIBRIDO, new BigDecimal("10"), new BigDecimal("100"), null);

        assertThrows(InvalidEfficiencyFactorException.class, () -> calculator.calculate(request));
    }

    @ParameterizedTest(name = "eficiencia={0} fuera de rango [0.1, 2.0] lanza InvalidEfficiencyFactorException")
    @CsvSource({"0.0", "0.05", "-1", "2.01", "5"})
    void rejectsEfficiencyFactorOutOfRange(BigDecimal efficiencyFactor) {
        CalculationRequest request = new CalculationRequest(
                VehicleType.HIBRIDO, new BigDecimal("10"), new BigDecimal("100"), efficiencyFactor);

        assertThrows(InvalidEfficiencyFactorException.class, () -> calculator.calculate(request));
    }

    @ParameterizedTest(name = "eficiencia límite válida={0} no lanza excepción")
    @CsvSource({"0.1", "2.0"})
    void acceptsEfficiencyFactorAtRangeBoundaries(BigDecimal efficiencyFactor) {
        CalculationRequest request = new CalculationRequest(
                VehicleType.HIBRIDO, new BigDecimal("10"), new BigDecimal("100"), efficiencyFactor);

        assertThat(calculator.calculate(request)).isPositive();
    }

    @Test
    @DisplayName("Valores muy grandes de peso y distancia se calculan sin overflow ni pérdida de precisión")
    void handlesLargeValuesWithoutOverflow() {
        BigDecimal hugeWeight = new BigDecimal("1000000");     // 1,000,000 t
        BigDecimal hugeDistance = new BigDecimal("1000000");   // 1,000,000 km
        BigDecimal efficiencyFactor = new BigDecimal("2.0");   // límite superior válido
        CalculationRequest request = new CalculationRequest(
                VehicleType.DIESEL, hugeWeight, hugeDistance, efficiencyFactor);

        BigDecimal expected = VehicleType.DIESEL.getEmissionFactorKgPerTonneKm()
                .multiply(hugeWeight)
                .multiply(hugeDistance)
                .multiply(efficiencyFactor);

        BigDecimal result = calculator.calculate(request);

        assertThat(result).isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("Solicitud nula lanza IllegalArgumentException (error de contrato, no de negocio)")
    void rejectsNullRequest() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(null));
    }
}
