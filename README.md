# ATM

| Proyecto | Descripción | Java | Framework |
|---|---|---|---|
| [`atm-backend`](atm-backend) | Servicio REST | 21 | Spring Boot 4.1.2 |
| [`atm-cli`](atm-cli) | Aplicación de consola | 8 | - |

## Instalación

Las instrucciones son para Windows, usando PowerShell.

- **JDK 21** para compilar y ejecutar `atm-backend`, y para ejecutar Gradle en ambos proyectos.
- **JDK 8** instalado para compilar, probar y ejecutar `atm-cli`, porque el proyecto usa una toolchain de Java 8.
- No hace falta instalar Gradle: cada proyecto trae su wrapper (`gradlew.bat`).
- Definir `JAVA_HOME` apuntando al JDK 21:

```powershell
$env:JAVA_HOME = "C:\ruta\al\jdk-21"
```

## Servicio REST (`atm-backend`)

```powershell
cd atm-backend

.\gradlew.bat test             # ejecutar tests unitarios
.\gradlew.bat integrationTest  # ejecutar tests de integración
.\gradlew.bat check            # ejecutar ambas suites y verificar la cobertura
.\gradlew.bat jacocoTestReport # generar el reporte de cobertura
.\gradlew.bat bootRun          # levantar el servicio
```

El servicio escucha en `http://localhost:8080`.

## Aplicación de consola (`atm-cli`)

El servicio debe estar levantado antes de usar la CLI.

```powershell
cd atm-cli

.\gradlew.bat test                    # ejecutar tests unitarios
.\gradlew.bat integrationTest         # ejecutar tests de integración
.\gradlew.bat check                   # ejecutar ambas suites y verificar la cobertura
.\gradlew.bat jacocoTestReport        # generar el reporte de cobertura
.\gradlew.bat run --console=plain     # ejecutar la aplicación
```

La aplicación muestra un menú y espera un comando. Con `0` termina.

```text
atm login <tarjeta>
atm extraer <tarjeta> <cuenta> <importe>
atm depositar <tarjeta> <cbu> <importe>
atm saldo <tarjeta> <cuenta>
0. Salir
```

Para usar otra URL del servicio, definir la variable de entorno `ATM_BACKEND_URL`.

## Datos de prueba

| Tarjeta | Estado |
|---|---|
| `1111111111111111` | Activa |
| `2222222222222222` | Inactiva |

| Cuenta | CBU | Saldo inicial | Tarjeta asociada |
|---|---|---|---|
| `00001` | `0000000000000000000001` | `10000,00` | `1111111111111111` |
| `00002` | `0000000000000000000002` | `0,00` | `1111111111111111` |
| `00003` | `0000000000000000000003` | `5000,00` | `2222222222222222` |

Los datos viven en memoria: al reiniciar el servicio, los saldos vuelven a estos valores.

## Reportes de cobertura

| Proyecto | Ruta |
|---|---|
| `atm-backend` | `atm-backend\build\reports\jacoco\test\html\index.html` |
| `atm-cli` | `atm-cli\build\reports\jacoco\test\html\index.html` |

## Log de auditoría

Con `bootRun`, los eventos se guardan en `atm-backend\logs\transactions.log`. El archivo se crea con el primer evento y su ruta se configura con la propiedad `atm.audit.file`.

## Documentación

- [`DESIGN.md`](DESIGN.md): decisiones de diseño y concesiones.
- [`TEST_EVIDENCES.md`](TEST_EVIDENCES.md): evidencias de las pruebas realizadas.
