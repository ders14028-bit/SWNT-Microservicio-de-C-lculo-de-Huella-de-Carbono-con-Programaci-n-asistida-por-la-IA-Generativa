package com.ecologistics.carbontracker.infrastructure.adapter.in.web;

import com.ecologistics.carbontracker.application.port.in.CalculateCarbonFootprintUseCase;
import com.ecologistics.carbontracker.infrastructure.adapter.in.web.mapper.CalculationRequestMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la capa web de {@link CarbonFootprintController} con {@code @WebMvcTest}:
 * solo carga el contexto MVC (controlador + {@link com.ecologistics.carbontracker.infrastructure.adapter.in.web.exception.GlobalExceptionHandler}),
 * mockeando el caso de uso para no ejecutar el dominio real. {@link CalculationRequestMapper}
 * se importa tal cual (es un {@code @Component} plano sin dependencias, no reconocido
 * automáticamente por el slice de {@code @WebMvcTest}).
 */
@WebMvcTest(CarbonFootprintController.class)
@Import(CalculationRequestMapper.class)
class CarbonFootprintControllerTest {

    private static final String CALCULATE_URL = "/api/v1/carbon-footprint/calculate";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CalculateCarbonFootprintUseCase calculateCarbonFootprintUseCase;

    @Test
    @DisplayName("POST con JSON válido devuelve 200 OK con el BigDecimal calculado")
    void validRequestReturnsOkWithCalculatedFootprint() throws Exception {
        given(calculateCarbonFootprintUseCase.calculate(any())).willReturn(new BigDecimal("180.0000"));

        String requestBody = """
                {
                    "vehicleType": "DIESEL",
                    "weightTonnes": 10,
                    "distanceKm": 100,
                    "efficiencyFactor": 1.2
                }
                """;

        mockMvc.perform(post(CALCULATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carbonFootprintKg").value(180.0));
    }

    @Test
    @DisplayName("Peso negativo dispara @Valid y devuelve 400 con el detalle del campo")
    void negativeWeightReturnsBadRequestWithFieldDetail() throws Exception {
        String requestBody = """
                {
                    "vehicleType": "DIESEL",
                    "weightTonnes": -5,
                    "distanceKm": 100,
                    "efficiencyFactor": 1.0
                }
                """;

        mockMvc.perform(post(CALCULATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details", hasItem(containsString("no puede ser negativo"))));
    }

    @Test
    @DisplayName("Distancia faltante dispara @Valid y devuelve 400 con el detalle del campo")
    void missingDistanceReturnsBadRequestWithFieldDetail() throws Exception {
        String requestBody = """
                {
                    "vehicleType": "DIESEL",
                    "weightTonnes": 10,
                    "efficiencyFactor": 1.0
                }
                """;

        mockMvc.perform(post(CALCULATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details", hasItem(containsString("La distancia es obligatoria"))));
    }

    @Test
    @DisplayName("weightTonnes con más de 9 dígitos enteros viola @Digits y devuelve 400")
    void weightWithTooManyIntegerDigitsReturnsBadRequest() throws Exception {
        String requestBody = """
                {
                    "vehicleType": "DIESEL",
                    "weightTonnes": 1234567890,
                    "distanceKm": 100,
                    "efficiencyFactor": 1.0
                }
                """;

        mockMvc.perform(post(CALCULATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("El peso admite máximo 9 dígitos"))));
    }

    @Test
    @DisplayName("distanceKm con más de 3 decimales viola @Digits y devuelve 400")
    void distanceWithTooManyFractionDigitsReturnsBadRequest() throws Exception {
        String requestBody = """
                {
                    "vehicleType": "DIESEL",
                    "weightTonnes": 10,
                    "distanceKm": 10.1234,
                    "efficiencyFactor": 1.0
                }
                """;

        mockMvc.perform(post(CALCULATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("La distancia admite máximo 9 dígitos"))));
    }

    @Test
    @DisplayName("JSON con sintaxis inválida devuelve 400 (ya no 500) gracias al handler de HttpMessageNotReadableException")
    void malformedJsonReturnsBadRequestNotInternalServerError() throws Exception {
        String malformedJson = "{ \"vehicleType\": \"DIESEL\", \"weightTonnes\": ";

        mockMvc.perform(post(CALCULATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "El cuerpo de la solicitud es inválido o contiene un valor no soportado."));
    }

    @Test
    @DisplayName("vehicleType fuera del enum devuelve 400, no 500")
    void unsupportedVehicleTypeValueReturnsBadRequest() throws Exception {
        String requestBody = """
                {
                    "vehicleType": "GASOLINA",
                    "weightTonnes": 10,
                    "distanceKm": 100,
                    "efficiencyFactor": 1.0
                }
                """;

        mockMvc.perform(post(CALCULATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Excepción no controlada del caso de uso devuelve 500 con mensaje genérico, sin detalles internos")
    void unhandledExceptionFromUseCaseReturnsInternalServerErrorWithGenericMessage() throws Exception {
        given(calculateCarbonFootprintUseCase.calculate(any()))
                .willThrow(new RuntimeException("detalle interno sensible que no debe exponerse"));

        String requestBody = """
                {
                    "vehicleType": "DIESEL",
                    "weightTonnes": 10,
                    "distanceKm": 100,
                    "efficiencyFactor": 1.0
                }
                """;

        mockMvc.perform(post(CALCULATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Ocurrió un error inesperado al procesar la solicitud."))
                .andExpect(jsonPath("$.message", not(containsString("detalle interno sensible"))));
    }
}
