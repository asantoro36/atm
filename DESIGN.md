## Sistema

- El sistema se compone de dos aplicaciones: `atm-cli` y `atm-backend`, comunicadas por REST.
- Del enunciado se interpreta que Java 8 aplica a la aplicación principal (`atm-cli`), mientras que la tecnología del servicio web es de libre elección. Por eso `atm-backend` usa Java 21 y Spring Boot.
- El servicio corre en localhost: escucha en `127.0.0.1` y la CLI lo consume mediante una URL `localhost`.

## atm-backend

- **Arquitectura:** Hexagonal
- **Organización**

```text
atm-backend (Java 21 · Spring Boot)
└── com.demo.atm
    ├── domain
    ├── application
    └── infrastructure
```

## atm-cli

- **Arquitectura:** MVP
- **Organización**

```text
atm
└── atm-cli (Java 8 · MVP)
    └── com.demo.atmcli
        ├── model
        ├── view
        └── presenter

```

## Tests
- **División de pruebas:** En los dos proyectos se opto por dividir test de integración y unitarios. 
- **WireMock en `atm-cli`:** Mockito no levanta un servidor HTTP, y WireMock permite probar el cliente REST real sin escribir un servidor fake.

## Consideraciones
- **Tarjeta y cuenta en `saldo`:** aunque el ejercicio contempla una relación 1 a 1, una tarjeta puede tener varias cuentas asociadas (por ejemplo, en pesos y en dólares). Pedir ambas conserva ese modelo de dominio y evita ambigüedad sobre qué cuenta se consulta.
- **Log de auditoría append-only:** Un registro de eventos al que solo se agregan líneas cubre la spec con una solución simple.
- **Registro de operaciones rechazadas:** auditar también los intentos fallidos permite reconstruir qué ocurrió, no solo los movimientos que cambiaron un saldo.
- **Evento obligatorio para mover dinero:** Se prioriza la trazabilidad sobre la disponibilidad.