## Sistema

- El sistema se compone de dos aplicaciones: `atm-cli` y `atm-backend`, comunicadas por HTTP.
- Del enunciado se interpreta que Java 8 aplica a la aplicación principal (`atm-cli`), mientras que la tecnología del servicio web es de libre elección. Por eso `atm-backend` usa Java 21 y Spring Boot.
- El servicio corre en localhost: escucha en `127.0.0.1` y la CLI lo consume mediante una URL `localhost`.

## atm-backend

- **Arquitectura:** hexagonal, para poder reemplazar la persistencia y la auditoría sin tocar el dominio.
- **API:** expone un contrato REST.
- **Organización**

```text
atm-backend (Java 21 · Spring Boot)
└── com.demo.atm
    ├── domain
    ├── application
    └── infrastructure
```

## atm-cli

- **Arquitectura:** MVP, para separar la consola de la lógica de presentación y del acceso al servicio.
- **Dependencias:** usa `HttpURLConnection` porque Java 8 no incluye un cliente HTTP moderno, y Gson para el JSON.
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

- **División de pruebas:** en los dos proyectos se optó por dividir tests de integración y unitarios.
- **WireMock en `atm-cli`:** Mockito no levanta un servidor HTTP, y WireMock permite probar el cliente REST real sin escribir un servidor fake.
- **Cobertura con JaCoCo:** se usa JaCoCo para medir la cobertura de los tests y exigir un mínimo de 80% de cobertura branch.

## Requisitos funcionales


| Requisito        | Cómo se cumple                                                                                                                       |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| Identificarse    | `atm login <tarjeta>` verifica que la tarjeta exista y esté activa, sin crear una sesión.                                            |
| Extraer          | `atm extraer <tarjeta> <cuenta> <importe>` debita de una cuenta asociada a la tarjeta y rechaza la operación si el saldo no alcanza. |
| Depositar        | `atm depositar <tarjeta> <cbu> <importe>` acredita en la cuenta del CBU, sea o no del dueño de la tarjeta.                           |
| Consultar saldo  | `atm saldo <tarjeta> <cuenta>` devuelve el saldo de una cuenta asociada a la tarjeta, sin modificarlo.                               |
| Entidades        | `Card`, `Account` y `CardAccount` modelan Tarjeta, Cuenta y TarjetaCuenta.                                                           |
| Consumo por HTTP | `atm-cli` llama a `atm-backend` en cada operación.                                                                                   |


## Requisitos no funcionales


| Requisito               | Cómo se cumple                                                                                                                                 |
| ----------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Trazabilidad            | Cada extracción, depósito y consulta procesados dejan un evento con su `transactionId`, incluso si se rechazan.                                |
| Auditoría de saldos     | El log registra fecha, tipo, resultado, motivo, cuenta e importe, con la tarjeta enmascarada.                                                  |
| Log en archivo de texto | Los eventos se agregan a un archivo JSON Lines, forzado a disco en cada escritura.                                                             |
| Servicio crítico        | Los reintentos, la idempotencia y los locks por cuenta protegen los saldos ante fallos de red y concurrencia, sin ofrecer alta disponibilidad. |


## Concesiones por falta de especificación

- **Tarjeta y cuenta en `saldo`:** se piden ambas porque una tarjeta puede tener varias cuentas asociadas, y así no hay ambigüedad sobre cuál se consulta.
- **Contrato HTTP:** el enunciado no define rutas ni códigos de respuesta, así que se diseñó un contrato propio.
- **Formatos:** se definieron tarjeta de 16 dígitos, cuenta de 5, CBU de 22 sin validar verificadores, e importe con coma decimal.
- **Consola:** cada operación se ejecuta tipeando el comando completo en una línea, sin pasos guiados, y la aplicación queda abierta esperando el siguiente.
- **Sesión:** el login no autoriza operaciones posteriores, por lo que cada comando vuelve a recibir la tarjeta.
- **Base de datos:** el enunciado la menciona, pero se usaron datos en memoria detrás de puertos para simplificar la entrega.
- **Tarjeta en depósitos:** debe existir y estar activa, aunque no necesita estar asociada a la cuenta.

## Decisiones

- **Idempotencia:** extracciones y depósitos usan una clave por comando, para que un reintento sin respuesta no ejecute la operación dos veces.
- **Concurrencia:** las operaciones se serializan por cuenta, para que dos pedidos simultáneos no dejen un saldo incorrecto o negativo.
- **Importes:** viajan como texto y se manejan con `BigDecimal`, para evitar errores de precisión.
- **Rechazos uniformes:** los rechazos de tarjeta o cuenta responden igual, para no revelar información sobre su validez.
- **Log de auditoría append-only:** un registro en JSON Lines al que solo se agregan líneas cubre la spec con una solución simple.
- **Login fuera de la auditoría:** el log existe para auditar saldos y el login no opera sobre ninguna cuenta; a cambio, no quedan registrados los intentos de ingreso.
- **Registro de operaciones rechazadas:** auditar también los intentos fallidos permite reconstruir qué ocurrió, no solo los movimientos que cambiaron un saldo.
- **Evento obligatorio para mover dinero:** se prioriza la trazabilidad sobre la disponibilidad, por lo que si no se puede auditar no se mueve dinero.

## Limitaciones

- **Disponibilidad:** aunque el servicio es crítico, corre en una sola instancia y no escala horizontalmente porque sus datos y locks están en memoria.
- **Datos en memoria:** al reiniciar se pierden saldos y claves de idempotencia, y el log se conserva, pero no se usa para reconstruirlos.
- **Consistencia del log:** una caída o un fallo de disco durante la escritura puede dejar una línea incompleta o un `SUCCESS` sin movimiento de saldo.
- **Crecimiento:** las claves y los locks en memoria nunca se liberan, y el archivo de auditoría no rota.
- **Seguridad:** la tarjeta es la única credencial y viaja por HTTP sin cifrar, algo aceptable solo porque el servicio corre en localhost.
- **URL del backend:** la CLI todavía no valida `ATM_BACKEND_URL`.

## Faltantes

- **Reversa de la extracción:** La extracción y el desposito no hacen una reversa de la operación si falla 
- **Nro de operación:** No se está devolviendo un número de operación para que el usuario pueda identificarla. Si se crea un transactionId. 