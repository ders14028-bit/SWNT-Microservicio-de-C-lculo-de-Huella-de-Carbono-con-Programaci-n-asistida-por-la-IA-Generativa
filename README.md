# Carbon Tracker Service

EcoLogistics es una empresa de logística que necesita estimar el impacto ambiental de sus operaciones de transporte de carga. Este repositorio contiene **Carbon Tracker Service**, un microservicio construido con Java 17 y Spring Boot 3, con arquitectura hexagonal (puertos y adaptadores), cuya función es calcular las emisiones de CO₂ (huella de carbono) producidas durante un envío logístico, a partir del tipo de vehículo utilizado, el peso de la carga, la distancia recorrida y un factor de eficiencia energética.

El proyecto se desarrolló de forma iterativa, con pair programming asistido por un modelo de lenguaje (LLM). La sección [10. Bitácora de prompts](#10-bitácora-de-prompts) documenta ese proceso paso a paso, con el prompt verbatim de cada fase.

---

## 1. Objetivo del proyecto

El microservicio resuelve un problema puntual: dado un envío de transporte de carga, calcular de forma determinista y auditable cuántos kilogramos de CO₂ equivalente se emitieron.

**Información que recibe:**
- Tipo de vehículo utilizado en el envío (`ELECTRICO`, `HIBRIDO`, `DIESEL`).
- Peso de la carga transportada (en toneladas).
- Distancia recorrida (en kilómetros).
- Factor de eficiencia energética del vehículo/operación (multiplicador adimensional).

**Resultado que produce:**
- Emisiones totales de CO₂ equivalente, en kilogramos, con precisión de 4 decimales.

El servicio no depende de base de datos ni de servicios externos: los factores de emisión están configurados internamente en el propio dominio.

## 2. Tecnologías utilizadas

- Java 17.
- Spring Boot 3.3 (Spring Web MVC).
- Maven.
- Jakarta Validation (Bean Validation).
- JUnit 5.
- Mockito.
- MockMvc.
- AssertJ (incluido vía `spring-boot-starter-test`).

No se incluye base de datos, seguridad, documentación OpenAPI ni contenedores en la versión actual del proyecto (ver [13. Posibles mejoras](#13-posibles-mejoras)).

## 3. Fórmula y reglas de negocio

### Fórmula

```
carbonFootprintKg = emissionFactor(vehicleType) × weightTonnes × distanceKm × efficiencyFactor
```

### Significado de cada variable

| Variable | Significado | Unidad |
|---|---|---|
| `weightTonnes` | Peso de la carga transportada | toneladas métricas (t) |
| `distanceKm` | Distancia recorrida en el envío | kilómetros (km) |
| `emissionFactor` | Emisión de referencia por tonelada-kilómetro, según el tipo de vehículo | kg CO₂ / t·km |
| `efficiencyFactor` | Multiplicador de eficiencia energética del vehículo u operación (adimensional) | sin unidad |
| `carbonFootprintKg` | Resultado final | kg CO₂eq |

### Factores de emisión por tipo de vehículo

| Vehículo (`VehicleType`) | Factor (kg CO₂ / t·km) |
|---|---|
| `ELECTRICO` | 0.02 |
| `HIBRIDO` | 0.08 |
| `DIESEL` | 0.15 |

> **Aviso importante:** estos factores son **valores académicos indicativos**, definidos para efectos de este proyecto y embebidos en el enum `VehicleType`. **No provienen de una fuente oficial** (por ejemplo, IPCC o GLEC Framework). En un sistema de producción real, estos valores deberían obtenerse y actualizarse desde una fuente oficial y verificable.

### Política de redondeo

- Toda la aritmética se realiza con `BigDecimal` (nunca `double`/`float`), para evitar errores de precisión.
- Al ser una operación puramente multiplicativa, `BigDecimal.multiply(...)` no pierde precisión en los pasos intermedios, por lo que **no se redondea en pasos intermedios**.
- El redondeo se aplica **una única vez**, sobre el resultado final, a **4 decimales**, usando `RoundingMode.HALF_UP`.

### Validaciones principales

Implementadas en dos niveles — Bean Validation en el borde HTTP (`CalculationRequestDto`) y reglas de negocio en el dominio (`CarbonFootprintCalculator`) —, como defensa en profundidad:

| Campo | Regla (Bean Validation, capa web) | Regla equivalente en el dominio |
|---|---|---|
| `vehicleType` | Obligatorio (`@NotNull`) | Nulo → `UnsupportedVehicleTypeException` |
| `weightTonnes` | Obligatorio; ≥ 0; máximo 9 dígitos enteros y 3 decimales (`@Digits`) | Negativo → `InvalidWeightException` (cero es válido) |
| `distanceKm` | Obligatorio; estrictamente > 0; máximo 9 dígitos enteros y 3 decimales (`@Digits`) | Nulo, negativo o cero → `InvalidDistanceException` |
| `efficiencyFactor` | Obligatorio; entre 0.1 y 2.0 inclusive | Nulo o fuera de `[0.1, 2.0]` → `InvalidEfficiencyFactorException` |

### Supuestos del proyecto

- La relación entre las variables es estrictamente multiplicativa; no hay términos aditivos.
- El `efficiencyFactor` es un multiplicador directo sobre el resultado: valores menores a 1.0 reducen la huella, mayores a 1.0 la aumentan; 1.0 representa un comportamiento nominal.
- Los factores de emisión son constantes fijas del dominio en esta versión; no varían por antigüedad del vehículo ni se consultan externamente.
- No se maneja conversión automática de unidades: se asume siempre entrada en toneladas y kilómetros.
- Un peso de carga de 0 toneladas es una entrada válida (produce 0 kg CO₂eq); una distancia de 0 km, en cambio, se rechaza explícitamente por regla de negocio.

## 4. Arquitectura y estructura del proyecto

```
carbon-tracker-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/ecologistics/carbontracker/
    │   │   ├── CarbonTrackerServiceApplication.java
    │   │   ├── domain/
    │   │   │   ├── CarbonFootprintCalculator.java
    │   │   │   ├── model/
    │   │   │   │   ├── VehicleType.java
    │   │   │   │   └── CalculationRequest.java
    │   │   │   └── exception/
    │   │   │       ├── CarbonCalculationException.java
    │   │   │       ├── InvalidDistanceException.java
    │   │   │       ├── InvalidWeightException.java
    │   │   │       ├── InvalidEfficiencyFactorException.java
    │   │   │       └── UnsupportedVehicleTypeException.java
    │   │   ├── application/
    │   │   │   ├── port/in/CalculateCarbonFootprintUseCase.java
    │   │   │   └── service/CarbonFootprintApplicationService.java
    │   │   └── infrastructure/
    │   │       ├── config/DomainConfig.java
    │   │       └── adapter/in/web/
    │   │           ├── CarbonFootprintController.java
    │   │           ├── dto/
    │   │           │   ├── CalculationRequestDto.java
    │   │           │   └── CalculationResponseDto.java
    │   │           ├── mapper/CalculationRequestMapper.java
    │   │           └── exception/
    │   │               ├── ApiErrorResponse.java
    │   │               └── GlobalExceptionHandler.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/com/ecologistics/carbontracker/
            ├── domain/CarbonFootprintCalculatorTest.java
            ├── application/service/CarbonFootprintApplicationServiceTest.java
            └── infrastructure/adapter/in/web/CarbonFootprintControllerTest.java
```

### Responsabilidades por capa

- **Dominio (`domain/`):** el núcleo hexagonal. `CarbonFootprintCalculator` aplica la fórmula y valida las reglas de negocio; no tiene ninguna dependencia de Spring, por lo que es testeable con JUnit puro. `VehicleType` es un enum que además guarda el factor de emisión de cada tipo. La jerarquía de excepciones (`CarbonCalculationException` y sus cuatro subtipos) representa cada regla de negocio violada.
- **Aplicación (`application/`):** capa de orquestación. `CalculateCarbonFootprintUseCase` es el puerto de entrada (driving port); `CarbonFootprintApplicationService` lo implementa delegando toda la lógica al dominio, sin agregar transformaciones ni reglas propias.
- **Infraestructura (`infrastructure/`):** detalles técnicos. `DomainConfig` da de alta como bean de Spring una clase de dominio que deliberadamente no usa anotaciones. El adaptador de entrada REST (`adapter/in/web`) traduce HTTP al caso de uso: DTOs con Bean Validation, un mapper DTO↔dominio, un controlador delgado y `GlobalExceptionHandler` (`@RestControllerAdvice`) como único punto de traducción de excepciones a respuestas HTTP uniformes.
- **Pruebas (`test/`):** organizadas con el mismo árbol de paquetes que el código de producción, una clase de test por clase relevante de cada capa.

Esta separación hace que cada clase tenga una única razón para cambiar: si cambia la fórmula o una regla de negocio, solo se toca el dominio; si cambia el contrato HTTP, solo se toca el adaptador web; si cambia la orquestación, solo se toca la capa de aplicación.

## 5. Funcionamiento del microservicio

1. El cliente envía una solicitud `POST` a `/api/v1/carbon-footprint/calculate` con el tipo de vehículo, el peso de la carga, la distancia y el factor de eficiencia.
2. Spring Boot deserializa el cuerpo JSON y Jakarta Validation (`@Valid`) verifica los campos obligatorios y sus rangos antes de que se ejecute cualquier lógica de negocio.
3. El controlador (`CarbonFootprintController`) mapea el DTO al modelo de dominio y delega en `CalculateCarbonFootprintUseCase`, sin realizar cálculos por sí mismo.
4. `CarbonFootprintApplicationService` delega directamente en `CarbonFootprintCalculator`.
5. El calculador valida las reglas de negocio (lanzando la excepción específica correspondiente si algo es inválido) y realiza el cálculo con `BigDecimal`, redondeando una única vez a 4 decimales.
6. La API devuelve una respuesta `200 OK` con las emisiones totales; ante datos inválidos, JSON malformado o errores internos, `GlobalExceptionHandler` devuelve una respuesta de error uniforme (`400` o `500`).

## 6. Endpoint de la API

### `POST /api/v1/carbon-footprint/calculate`

Calcula la huella de carbono de un envío de transporte de carga.

**Campos de entrada:**

| Campo | Tipo | Validaciones |
|---|---|---|
| `vehicleType` | `string` (uno de: `ELECTRICO`, `HIBRIDO`, `DIESEL`) | Obligatorio |
| `weightTonnes` | `number` (decimal) | Obligatorio; ≥ 0; máximo 9 dígitos enteros y 3 decimales |
| `distanceKm` | `number` (decimal) | Obligatorio; > 0; máximo 9 dígitos enteros y 3 decimales |
| `efficiencyFactor` | `number` (decimal) | Obligatorio; entre 0.1 y 2.0 |

**Ejemplo de solicitud:**

```json
{
    "vehicleType": "DIESEL",
    "weightTonnes": 10,
    "distanceKm": 100,
    "efficiencyFactor": 1.2
}
```

**Ejemplo de respuesta exitosa (`200 OK`):**

```json
{
    "carbonFootprintKg": 180.0000
}
```

**Ejemplo de respuesta de error (`400 Bad Request`, peso de carga negativo):**

```json
{
    "timestamp": "2026-08-26T20:00:00Z",
    "status": 400,
    "error": "Bad Request",
    "message": "La solicitud contiene datos inválidos.",
    "details": [
        "El peso de la carga no puede ser negativo."
    ]
}
```

**Códigos de estado HTTP posibles:**

| Código | Motivo |
|---|---|
| `200 OK` | Cálculo realizado correctamente |
| `400 Bad Request` | Datos inválidos (violación de Jakarta Validation), JSON malformado, `vehicleType` inexistente en el enum, o violación de una regla de negocio del dominio |
| `500 Internal Server Error` | Error interno no controlado (se registra en el log del servidor sin exponer detalle al cliente) |

## 7. Instalación y ejecución

1. **Clonar el repositorio:**

   ```bash
   git clone https://github.com/ders14028-bit/SWNT-Microservicio-de-C-lculo-de-Huella-de-Carbono-con-Programaci-n-asistida-por-la-IA-Generativa.git
   ```

2. **Entrar a la carpeta del proyecto:**

   ```bash
   cd SWNT-Microservicio-de-C-lculo-de-Huella-de-Carbono-con-Programaci-n-asistida-por-la-IA-Generativa
   ```

3. **Verificar la versión de Java** (se requiere Java 17):

   ```bash
   java -version
   ```

4. **Compilar el proyecto:**

   ```bash
   mvn clean compile
   ```

5. **Ejecutar las pruebas:**

   ```bash
   mvn test
   ```

6. **Iniciar el microservicio:**

   ```bash
   mvn spring-boot:run
   ```

   El servicio queda disponible en `http://localhost:8080` (puerto configurado en `application.yml`).

7. **Probar el endpoint:**

   - Linux/macOS:
     ```bash
     curl -X POST http://localhost:8080/api/v1/carbon-footprint/calculate \
       -H "Content-Type: application/json" \
       -d '{"vehicleType":"DIESEL","weightTonnes":10,"distanceKm":100,"efficiencyFactor":1.2}'
     ```
   - Windows (PowerShell):
     ```powershell
     Invoke-RestMethod -Uri http://localhost:8080/api/v1/carbon-footprint/calculate -Method Post -ContentType "application/json" -Body '{"vehicleType":"DIESEL","weightTonnes":10,"distanceKm":100,"efficiencyFactor":1.2}'
     ```

## 8. Código fuente

El código completo del microservicio está organizado dentro de `src/main/java/com/ecologistics/carbontracker`. Las clases principales son:

- `CarbonTrackerServiceApplication`: punto de entrada de Spring Boot.
- `domain.CarbonFootprintCalculator`: fórmula y reglas de negocio, sin dependencias de Spring.
- `domain.model.VehicleType`: enum de tipos de vehículo soportados, cada uno con su factor de emisión.
- `domain.model.CalculationRequest`: contrato de entrada puro del dominio.
- `domain.exception.*`: jerarquía de excepciones de negocio (`CarbonCalculationException` y sus cuatro subtipos).
- `application.port.in.CalculateCarbonFootprintUseCase` / `application.service.CarbonFootprintApplicationService`: puerto e implementación de la orquestación.
- `infrastructure.adapter.in.web.CarbonFootprintController`: expone el endpoint REST.
- `infrastructure.adapter.in.web.dto.*`: DTOs inmutables de entrada y salida con Bean Validation.
- `infrastructure.adapter.in.web.mapper.CalculationRequestMapper`: traduce entre DTOs y modelo de dominio.
- `infrastructure.adapter.in.web.exception.ApiErrorResponse` / `GlobalExceptionHandler`: manejo uniforme de errores.
- `infrastructure.config.DomainConfig`: cablea el calculador de dominio como bean de Spring.

Fragmento representativo del cálculo (archivo completo en [`CarbonFootprintCalculator.java`](src/main/java/com/ecologistics/carbontracker/domain/CarbonFootprintCalculator.java)):

```java
return request.vehicleType().getEmissionFactorKgPerTonneKm()
        .multiply(request.weightTonnes())
        .multiply(request.distanceKm())
        .multiply(request.efficiencyFactor())
        .setScale(RESULT_SCALE, RoundingMode.HALF_UP);
```

El código completo, con la implementación real de cada clase, permanece en los archivos del repositorio; este README no lo reproduce en su totalidad.

## 9. Suite de pruebas

**Herramientas utilizadas:** JUnit 5, Mockito, MockMvc (vía `spring-boot-starter-test`), AssertJ (incluido en el mismo starter).

**Ubicación de los archivos de prueba:**

- `src/test/java/com/ecologistics/carbontracker/domain/CarbonFootprintCalculatorTest.java`
- `src/test/java/com/ecologistics/carbontracker/application/service/CarbonFootprintApplicationServiceTest.java`
- `src/test/java/com/ecologistics/carbontracker/infrastructure/adapter/in/web/CarbonFootprintControllerTest.java`

**Diferencia entre las tres suites:**

- **Pruebas de dominio (`CarbonFootprintCalculatorTest`):** no usan Mockito, porque `CarbonFootprintCalculator` no tiene colaboradores externos que mockear. Usan `@ParameterizedTest` (`@MethodSource`/`@CsvSource`) para reducir duplicación.
- **Pruebas del servicio de aplicación (`CarbonFootprintApplicationServiceTest`):** único punto donde se usa Mockito con `@Mock`/`@InjectMocks`, para aislar al orquestador puro del dominio real.
- **Pruebas del controlador (`CarbonFootprintControllerTest`):** usan `@WebMvcTest` + `@MockBean` sobre `CalculateCarbonFootprintUseCase` para aislar la capa HTTP, verificando serialización, códigos de estado y que `GlobalExceptionHandler` intercepte correctamente las solicitudes inválidas.

**Casos cubiertos:**

- Caso feliz por cada `VehicleType` (ELECTRICO, HIBRIDO, DIESEL), con valores esperados calculados manualmente.
- Peso en cero (válido); distancia nula/cero/negativa y peso negativo (rechazados); tipo de vehículo nulo (rechazado); factor de eficiencia nulo y fuera de `[0.1, 2.0]` (rechazado), incluyendo los límites exactos válidos.
- Valores extremos (peso y distancia de 1,000,000) sin overflow de `BigDecimal`.
- Delegación y propagación de excepciones sin transformar en el servicio de aplicación, verificada con `verify(...)` y `assertSame(...)`.
- A nivel HTTP: caso feliz `200 OK`; validación `@Valid` (peso negativo, distancia faltante); violación de `@Digits`; JSON malformado (`400`, ya no `500`); `vehicleType` fuera del enum (`400`); excepción genérica no controlada (`500` con mensaje genérico, sin exponer el detalle interno).

**Comando para ejecutar las pruebas:**

```bash
mvn test
```

**Resultado real de la última ejecución** (`mvn test`, verificado en este entorno de desarrollo):

| Clase de test | Tests ejecutados | Fallos | Errores |
|---|---|---|---|
| `CarbonFootprintCalculatorTest` | 23 | 0 | 0 |
| `CarbonFootprintApplicationServiceTest` | 3 | 0 | 0 |
| `CarbonFootprintControllerTest` | 8 | 0 | 0 |
| **Total** | **34** | **0** | **0** |

**Cobertura de código:** no se configuró ninguna herramienta de cobertura (por ejemplo, JaCoCo) en esta versión (ver [13. Posibles mejoras](#13-posibles-mejoras)).

## 10. Bitácora de prompts

Documentación del proceso de desarrollo asistido por LLM, fase por fase: el prompt usado (verbatim), el resumen de la respuesta/decisión clave del LLM, y los ajustes posteriores (refinamiento iterativo).

### Fase 1 — Contexto y razonamiento previo al código

**Prompt 1 (rol y estándares):**
```text
Actúa como un Desarrollador Senior de Java especializado en arquitecturas de microservicios.
Stack: Java 17 + Spring Boot 3, Maven, arquitectura hexagonal (puertos y adaptadores).
Estándares: Clean Code, principios SOLID, inyección de dependencias, manejo de excepciones
con @ControllerAdvice, validación con Bean Validation (jakarta.validation).
Contexto del dominio: microservicio "Carbon Tracker Service" para EcoLogistics, que calcula
emisiones de CO2 de envíos logísticos según tipo de vehículo (ELÉCTRICO, DIÉSEL, HÍBRIDO),
peso de carga (toneladas), distancia (km) y factor de eficiencia energética.
A partir de ahora todas mis solicitudes se enmarcan en este contexto. Confirma que entendiste
el rol y espera mis instrucciones paso a paso.
```

*Decisión clave del LLM:* confirmación del rol y del marco de trabajo (stack, arquitectura,
estándares) sin generar código todavía — se estableció el contrato de la sesión.

**Prompt 2 (Chain-of-Thought antes de codificar):**
```text
Antes de escribir código, razona paso a paso cómo calcularías la huella de carbono:
1. ¿Qué variables intervienen y qué unidades usa cada una?
2. ¿Cómo varía el factor de emisión (kg CO2/km/tonelada) según tipo de vehículo?
3. ¿Cómo se relaciona el "factor de eficiencia" con el resultado final: multiplica, divide,
   ajusta el factor base?
4. Propón la fórmula matemática final explícita.
5. Enumera los casos borde que la lógica debe contemplar (valores en cero, negativos,
   tipos de vehículo no soportados, valores nulos).
No escribas código todavía, solo el razonamiento y la fórmula.
```

*Decisión clave del LLM:* fijó la fórmula `emissionFactor × peso × distancia × eficiencia`
(multiplicación, no división, para que el factor de eficiencia amplifique o reduzca
emisiones respecto a un comportamiento nominal = 1.0); propuso factores de emisión base
indicativos por tipo de vehículo (ELÉCTRICO < HÍBRIDO < DIÉSEL); y enumeró los casos borde
(nulos, cero, negativos, tipo no soportado, precisión numérica) que luego se convirtieron
directamente en las reglas de validación de `CarbonFootprintCalculator`.

*Ajuste posterior:* ninguno en esta fase — se confirmó ("esta bien") y la fórmula/razonamiento
pasaron sin cambios a la fase de implementación.

### Fase 2 — Implementación del dominio y modularización en capas

**Prompt 3 (dominio puro):**
```text
Con base en la fórmula que definiste, implementa una clase de dominio CarbonFootprintCalculator
con un método calculate(CalculationRequest request) que devuelva las emisiones en kg CO2.
No uses anotaciones de Spring aquí — debe ser una clase de dominio pura, testeable sin contexto
de Spring.
```

*Decisión clave del LLM:* generó `CarbonFootprintCalculator`, `VehicleType` (enum con factor de
emisión embebido) y `CalculationRequest` (record) usando `BigDecimal` (no `double`) para evitar
errores de redondeo, con una única excepción genérica `InvalidCalculationRequestException` para
todos los casos inválidos.

**Prompt 4 (refinamiento iterativo — excepciones específicas):**
```text
Revisa el método anterior y refuerza el manejo de errores: lanza excepciones de dominio
específicas (no genéricas) para: distancia negativa o cero, peso negativo, tipo de vehículo
nulo o no soportado, factor de eficiencia fuera de rango válido. Define esas excepciones
como clases propias que extiendan RuntimeException.
```

*Ajuste realizado:* se eliminó `InvalidCalculationRequestException` y se reemplazó por una
jerarquía específica — `CarbonCalculationException` (abstracta) con cuatro subtipos:
`InvalidDistanceException`, `InvalidWeightException`, `InvalidEfficiencyFactorException`,
`UnsupportedVehicleTypeException` —, cada una lanzada por un método de validación dedicado
dentro del calculador. Este es el único refinamiento iterativo real de la Fase 2: pasar de
"una excepción para todo" a "una excepción por regla de negocio violada".

**Prompt 5 (modularización hexagonal):**
```text
Separa esta lógica en capas: dominio (modelo + lógica de cálculo + excepciones), servicio
de aplicación (orquestación), y controlador REST (DTOs de entrada/salida, validación con
@Valid, manejo de errores con @RestControllerAdvice). Propón la estructura de carpetas
siguiendo arquitectura hexagonal y explica brevemente el rol de cada paquete.
```

*Decisión clave del LLM:* introdujo el puerto de entrada `CalculateCarbonFootprintUseCase` y
su implementación `CarbonFootprintApplicationService` (capa `application`), y el adaptador de
entrada REST en `infrastructure.adapter.in.web` (controlador, DTOs, mapper,
`GlobalExceptionHandler`). Decisión explícita de **no** crear un puerto de salida (`port/out`):
el cálculo no depende de persistencia ni de ningún sistema externo, así que agregar esa
abstracción habría sido sobre-ingeniería sin consumidor real.

### Fase 3 — Testing y Code Review

**Prompt 6 (tests de dominio):**
```text
Genera una suite de pruebas con JUnit 5 + Mockito para CarbonFootprintCalculator. Cubre:
casos normales para cada tipo de vehículo, distancia = 0, peso negativo, tipo de vehículo
no soportado, factor de eficiencia nulo o inválido, valores límite (muy grandes). Usa
@ParameterizedTest donde aplique para reducir duplicación.
```

*Ajuste de alcance:* el LLM señaló que `CarbonFootprintCalculator` no tiene colaboradores
externos, por lo que Mockito no aplica ahí — se generaron 23 pruebas con JUnit 5 puro
(`@ParameterizedTest` + `@MethodSource`/`@CsvSource` para los 5 grupos con variación de
datos), sin un solo `@Mock`.

**Prompt 7 (tests del application service, con Mockito):**
```text
Genera pruebas con JUnit 5 + Mockito para esta clase:

[CarbonFootprintApplicationService.java]

La clase es un orquestador puro: implementa CalculateCarbonFootprintUseCase y delega
directamente en CarbonFootprintCalculator sin transformar datos ni agregar lógica propia.

Cubre:
1. Caso feliz: dado un CalculationRequest y un BigDecimal simulado como retorno del
   calculador, verifica que calculate() del service devuelve exactamente ese mismo valor.
2. Verifica con verify(calculator, times(1)).calculate(request) que el service delega
   correctamente y pasa el mismo objeto request sin modificarlo.
3. Propagación de excepciones: si calculator.calculate() lanza una excepción de dominio
   (ej. InvalidVehicleTypeException u otra que definiste), el service NO debe capturarla
   ni transformarla — debe propagarse tal cual. Verifica esto con assertThrows.
4. Usa @ExtendWith(MockitoExtension.class), @Mock para CarbonFootprintCalculator,
   @InjectMocks para el service.

No agregues pruebas de mapeo de DTOs ni de logging, ya que la clase no realiza esas
operaciones.
```

*Ajuste de alcance (relevante para la reflexión de la sección 12):* el prompt restringió
explícitamente al LLM para que no probara mapeo de DTOs ni logging — dos operaciones que,
en una capa de aplicación típica, un LLM tendería a asumir por defecto. Sin esa restricción
explícita, el riesgo de que el modelo generara mocks/asserts para comportamiento inexistente
(sobre-ingeniería de tests) era real. Con la restricción, la suite resultante fue de 3
pruebas (`@ExtendWith(MockitoExtension.class)`, `@Mock` para el calculador, `@InjectMocks`
para el service), exactamente alineada con lo que la clase realmente hace.

**Prompt 8 (code review, sesión nueva):**
```text
Actúa como un revisor de código senior enfocado en seguridad y rendimiento. Aquí está el
código de un microservicio Spring Boot [código completo]. Identifica: vulnerabilidades
de validación de entrada, riesgos de rendimiento (ej. objetos innecesarios, cálculos
repetidos), violaciones de SOLID, y sugiere mejoras concretas con ejemplos de código.
```

*Hallazgos clave del LLM (revisión crítica, no genérica):*
- **Crítico:** `CalculationRequestDto` no acotaba el tamaño de los campos `BigDecimal`
  (solo `@DecimalMin`) — un atacante podía enviar un número con miles de dígitos de
  escala/precisión y degradar CPU/memoria del servicio en cada `.multiply()` del dominio
  (DoS algorítmico, bajo esfuerzo / alto impacto).
- **Alto:** `HttpMessageNotReadableException` (JSON malformado o valor de enum inválido)
  no estaba manejada en `GlobalExceptionHandler` → caía en el `catch-all` de `Exception` y
  devolvía `500` en vez de `400` (semántica HTTP incorrecta).
- **Alto:** el handler genérico de `Exception` no registraba nada — cero visibilidad en
  servidor ante errores o abuso del endpoint.
- **Medio/vigilar, no urgente:** endpoint sin autenticación/rate-limit (decisión de
  topología de despliegue, no defecto de código); `Arrays.toString(VehicleType.values())`
  recalculado en cada validación fallida (micro-optimización, bajo impacto); tensión menor
  de DIP (el service depende de la clase concreta `CarbonFootprintCalculator`, no de una
  interfaz) y de OCP (factores de emisión hardcodeados en el `enum`) — ambos marcados
  explícitamente como *"vigilar, no urgente"* por no haber una necesidad de negocio concreta
  hoy que los justifique.

**Prompt 9 (aplicación de fixes priorizados):**
```text
Aplica los fixes 1, 2 y 3 de tu revisión directamente al código:

1. Agrega @Digits(integer = 9, fraction = 3) a weightTonnes y distanceKm en
   CalculationRequestDto, manteniendo las anotaciones @NotNull/@DecimalMin existentes.

2. Agrega un @ExceptionHandler(HttpMessageNotReadableException.class) en
   GlobalExceptionHandler que devuelva 400 Bad Request con un ApiErrorResponse,
   colocado antes del handler genérico de Exception.class.

3. Agrega un Logger (SLF4J) a GlobalExceptionHandler y registra con log.error(...)
   dentro del @ExceptionHandler(Exception.class) antes de construir la respuesta.

Muéstrame el diff completo de cada archivo modificado.
```

*Resultado:* los tres fixes se aplicaron literalmente como se pidió, se compiló y se corrió
la suite completa (34 pruebas) para confirmar que no se rompió nada existente.

**Prompt 10 (tests de verificación de los fixes):**
```text
Genera pruebas @WebMvcTest para CarbonFootprintController, usando MockMvc.
Mockea CalculateCarbonFootprintUseCase con @MockBean.

Cubre:
1. Caso feliz: POST con JSON válido → 200 OK con el BigDecimal esperado en el body.
2. Validación @Valid: peso o distancia negativos/faltantes → 400 Bad Request con el
   detalle de campos inválidos en ApiErrorResponse.
3. weightTonnes o distanceKm con más dígitos que los permitidos por @Digits(integer=9,
   fraction=3) → 400 Bad Request.
4. JSON malformado (sintaxis inválida) → 400 Bad Request (verifica que ya NO cae en 500,
   validando el fix de HttpMessageNotReadableException).
5. vehicleType con un valor fuera del enum (ej. "GASOLINA") → 400 Bad Request.
6. Excepción de dominio no controlada específicamente (simula que el use case lanza una
   RuntimeException genérica) → 500 Internal Server Error con mensaje genérico, sin
   exponer detalles internos en el body.

Usa @Autowired MockMvc, given()/willReturn() o when()/thenReturn() de Mockito, y
verifica tanto el status HTTP como campos clave del body con jsonPath.
```

*Ajuste técnico durante la implementación:* `@WebMvcTest` solo auto-descubre `@Controller`,
`@ControllerAdvice`, `Converter`, `Filter`, etc. — no beans `@Component` planos como
`CalculationRequestMapper`, que el controlador necesita como dependencia real. Se agregó
`@Import(CalculationRequestMapper.class)` junto al `@MockBean` del caso de uso para que el
contexto de prueba cargara correctamente. Resultado: **8/8 pruebas en verde**, incluyendo la
verificación explícita (`not(containsString(...))`) de que el mensaje interno de una
excepción simulada nunca llega al body de la respuesta.

## 11. Revisión crítica del código

La revisión de seguridad y rendimiento se realizó en una **sesión nueva del LLM**, actuando explícitamente como revisor senior, sobre el código completo ya modularizado (no durante la generación inicial). Hallazgos principales:

| Hallazgo | Severidad | Riesgo | Corrección aplicada | Estado |
|---|---|---|---|---|
| `CalculationRequestDto` sin límite de tamaño en los campos `BigDecimal` (solo `@DecimalMin`) | Crítica | DoS algorítmico: un número con miles de dígitos de escala/precisión degrada CPU/memoria en cada `.multiply()` del dominio | Se agregó `@Digits(integer = 9, fraction = 3)` a `weightTonnes` y `distanceKm` | Aplicada |
| `HttpMessageNotReadableException` (JSON malformado o `vehicleType` fuera del enum) no manejada en `GlobalExceptionHandler` | Alta | Caía en el `catch-all` de `Exception` y devolvía `500` en vez de `400` (semántica HTTP incorrecta) | Se agregó un `@ExceptionHandler(HttpMessageNotReadableException.class)` dedicado, antes del handler genérico | Aplicada |
| El handler genérico de `Exception` no registraba nada en el log del servidor | Alta | Cero visibilidad ante errores o abuso del endpoint | Se agregó un `Logger` SLF4J y `log.error(...)` dentro del handler, sin exponer el detalle al cliente | Aplicada |
| Endpoint sin autenticación/rate-limit; `Arrays.toString(VehicleType.values())` recalculado en cada validación fallida; `CarbonFootprintApplicationService` depende de la clase concreta `CarbonFootprintCalculator` (no de una interfaz); factores de emisión hardcodeados en el `enum` | Media/Baja | Riesgos y tensiones de diseño reales, pero sin necesidad de negocio concreta hoy que los justifique | No aplicada | Marcada explícitamente como "vigilar, no urgente" |

Cada uno de los tres fixes de severidad alta/crítica se verificó con pruebas `@WebMvcTest` nuevas (incluidas en las 34 pruebas reportadas en la sección 9), no solo con la afirmación del LLM de que el problema quedó resuelto.

## 12. Reflexión crítica sobre el uso de IA generativa

Usar un LLM en este proceso aceleró tareas mecánicas (boilerplate de capas hexagonales, DTOs con Bean Validation, suites `@ParameterizedTest`) sin que esto sustituyera el criterio del equipo, porque cada entrega se revisó y se ejecutó contra el compilador y los tests reales, no solo contra la explicación del modelo. El riesgo concreto de sobre-ingeniería se vio al pedir las pruebas del `CarbonFootprintApplicationService`: sin restringir explícitamente el alcance a lo que la clase realmente hace, un LLM tiende a asumir por defecto que una capa de aplicación mapea DTOs o registra logs, generando mocks y asserts para comportamiento que la clase no tiene. En contraste, la sesión de code review dedicada (con contexto nuevo y rol de "revisor senior") encontró un hallazgo real y no trivial que ninguna etapa anterior había señalado: el DoS algorítmico por permitir `BigDecimal` sin límite de dígitos en el DTO de entrada. Esto confirma que el mayor valor del LLM no fue generar código por primera vez, sino actuar como una segunda mirada crítica y adversarial cuando se le pidió explícitamente ese rol — mientras que su valor como generador por defecto exige siempre acotar el alcance con instrucciones precisas y verificar cada resultado compilando y ejecutando las pruebas, no aceptándolo por confianza.

## 13. Posibles mejoras

Las siguientes mejoras **no forman parte de la versión actual** del proyecto:

- Obtener los factores de emisión desde una fuente oficial (por ejemplo, IPCC o GLEC Framework), en lugar de valores indicativos embebidos en el enum.
- Extraer un puerto de configuración para los factores de emisión, de modo que puedan actualizarse sin recompilar (`@ConfigurationProperties`), si el ritmo de cambio regulatorio lo justifica.
- Agregar autenticación, autorización y/o rate-limiting al endpoint.
- Incorporar documentación OpenAPI/Swagger.
- Crear una imagen Docker para el despliegue del microservicio.
- Implementar integración continua (por ejemplo, GitHub Actions) que compile y ejecute las pruebas automáticamente.
- Configurar y ejecutar una herramienta de cobertura de código (por ejemplo, JaCoCo).

## 14. Autor

- **Nombre del estudiante:** Daniel Esteban Rodriguez Suarez
