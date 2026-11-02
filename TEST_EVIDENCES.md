# Evidencias de pruebas

## Entorno

| Elemento | Versión |
|---|---|
| Sistema operativo | Windows 11 Home |
| JDK de `atm-backend` y de Gradle | 21.0.7 |
| JDK de `atm-cli` | 8 (Corretto 1.8.0_512) |
| Gradle | 9.8.2 |

## Cómo reproducir

```bash
cd atm-backend && ./gradlew check jacocoTestReport
cd atm-cli && ./gradlew check jacocoTestReport
```

El reporte de cobertura queda en `build/reports/jacoco/test/html/index.html` de cada proyecto.

## Resultados

Corrida completa del 01/11/2026. Ningún test falló ni se omitió.

| Proyecto | Unitarios | Integración | Total |
|---|---|---|---|
| `atm-backend` | 222 | 224 | 446 |
| `atm-cli` | 132 | 78 | 210 |
| **Total** | **354** | **302** | **656** |

## Cobertura

Medida con JaCoCo, combinando tests unitarios y de integración. El mínimo exigido es 80% de cobertura branch.

| Proyecto | Branch | Instrucciones |
|---|---|---|
| `atm-backend` | 100% (59 de 59) | 99% |
| `atm-cli` | 100% (61 de 61) | 90% |

## Prueba de punta a punta

`atm-cli` contra `atm-backend` real, con los datos iniciales del sistema.

| Comando | Resultado |
|---|---|
| `atm login 1111111111111111` | Ingreso exitoso |
| `atm login 2222222222222222` | Ingreso no exitoso |
| `atm login 9999999999999999` | Ingreso no exitoso |
| `atm saldo 1111111111111111 00001` | Su saldo es $ 10000,00 |
| `atm extraer 1111111111111111 00001 100,50` | Retire su dinero |
| `atm saldo 1111111111111111 00001` | Su saldo es $ 9899,50 |
| `atm extraer 1111111111111111 00002 100,00` | Saldo insuficiente |
| `atm extraer 1111111111111111 00001 0` | Importe inválido |
| `atm extraer 2222222222222222 00003 10,00` | Operación rechazada |
| `atm extraer 1111111111111111 00003 10,00` | Operación rechazada |
| `atm depositar 1111111111111111 0000000000000000000003 250,75` | Depósito exitoso |
| `atm depositar 2222222222222222 0000000000000000000001 10,00` | Operación rechazada |
| `atm saldo 1111111111111111 00003` | Operación rechazada |
| `atm saldo 1111111111111111 123` | Operación rechazada |
| `comando cualquiera` | Comando inválido |

## Servicio no disponible

| Comando | Resultado |
|---|---|
| `atm login` | Ingreso no exitoso |
| `atm saldo` | Servicio no disponible |
| `atm extraer` | No se pudo confirmar la operación. Consulte su saldo |
| `atm depositar` | No se pudo confirmar la operación |

## Log de auditoría

Los eventos se guardan en `atm-backend/logs/transactions.log`, que no se versiona: el archivo se crea al ejecutar operaciones y, si se borra, se genera de nuevo sin los eventos anteriores.
