# ATM


| Proyecto | Descripción | Java | Framework |
|---|---|---|---|
| [`atm-backend`](atm-backend) | Servicio REST | 21 | Spring Boot 4.1.2 |
| [`atm-cli`](atm-cli) | Aplicación de consola | 8 | - |

## Requisitos

- **JDK 21** para compilar y ejecutar `atm-backend`.
- **JDK 17 o superior** para ejecutar Gradle en `atm-cli` (Gradle 9 no corre sobre Java 8). El código se compila con `--release 8`, y el jar resultante corre en una JVM 8.
- No hace falta instalar Gradle: cada proyecto trae su wrapper (`gradlew` / `gradlew.bat`).
- Definir `JAVA_HOME` apuntando al JDK 21:

```powershell
# Windows (PowerShell)
$env:JAVA_HOME = "C:\ruta\al\jdk-21"
```
```bash
# Linux / macOS / Git Bash
export JAVA_HOME=/ruta/al/jdk-21
```

En Windows usar `gradlew.bat` en lugar de `./gradlew`.

## Servicio REST (`atm-backend`)

```bash
cd atm-backend

./gradlew test             # ejecutar tests unitarios
./gradlew integrationTest  # ejecutar tests de integración
./gradlew bootRun          # levantar el servicio
```



## Aplicación de consola (`atm-cli`)

El servicio debe estar levantado antes de usar la CLI.

```bash
cd atm-cli

./gradlew test                    # ejecutar tests unitarios
./gradlew integrationTest         # ejecutar tests de integración
./gradlew run --console=plain     # ejecutar la aplicación
```



## Documentación

- [`DESIGN.md`](DESIGN.md): decisiones de diseño y concesiones.
- [`TEST_EVIDENCES.md`](TEST_EVIDENCES.md): evidencias de las pruebas realizadas.
