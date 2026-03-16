# 📱 SmsSender

**SmsSender** es una aplicación Android que convierte tu teléfono en un **gateway SMS HTTP**. Expone un servidor HTTP local en la red WiFi del dispositivo, permitiendo enviar SMS de forma programática mediante peticiones REST desde cualquier cliente externo.

---

## ✨ Características

- 🌐 **Servidor HTTP embebido** (NanoHTTPD) que escucha peticiones REST locales
- 📤 **Envío de SMS** a través de la API nativa de Android
- 🔐 **Autenticación mediante API Key** en cabecera HTTP
- 📶 **Soporte multi-SIM**: selección de ranura SIM por petición o configuración global
- 🔔 **Servicio en primer plano** con notificación persistente
- 📋 **Registro de logs** en tiempo real desde la interfaz
- ⚙️ **Configuración persistente**: puerto, API Key y SIM predeterminada guardados en SharedPreferences
- 🎨 **Interfaz moderna** con Jetpack Compose y Material 3

---

## 🏗️ Arquitectura

El proyecto sigue la **Arquitectura Hexagonal (Ports & Adapters)**:

```
org.aref.smssender
├── domain/
│   ├── model/          → SmsMessage, SmsResult, SimInfo
│   └── port/
│       ├── input/      → SendSmsUseCase (puerto de entrada)
│       └── output/     → SmsSenderPort (puerto de salida)
├── application/
│   └── service/        → SendSmsService (caso de uso)
├── infrastructure/
│   ├── adapter/
│   │   ├── input/
│   │   │   └── http/   → HttpServerAdapter (NanoHTTPD) + DTOs
│   │   └── output/
│   │       └── sms/    → AndroidSmsSenderAdapter
│   ├── config/         → AppConfig (SharedPreferences)
│   ├── log/            → SmsLog
│   └── service/        → SmsServerService (Foreground Service)
└── ui/                 → MainActivity + Composables
```

---

## 🛠️ Stack tecnológico

| Componente | Tecnología / Versión |
|---|---|
| Lenguaje | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| HTTP Server | NanoHTTPD 2.3.1 |
| JSON | Gson 2.11.0 |
| Min SDK | Android 10 (API 29) |
| Target SDK | Android 16 (API 36) |
| AGP | 8.13.2 |

---

## 🚀 Instalación y uso

### 1. Clonar y compilar

```bash
git clone https://github.com/ronaldfelix/sms-sender.git
cd SmsSender
./gradlew assembleDebug
```

### 2. Instalar en el dispositivo via adb

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. Configurar la app

1. Abre la aplicación en el dispositivo.
2. Se genera automáticamente una **API Key** aleatoria (UUID).
3. Configura el **puerto** si lo deseas (por defecto: `8080`).
4. Selecciona la **SIM predeterminada** si el dispositivo tiene dual-SIM.
5. Pulsa **Iniciar servidor**.

---

## 📡 API REST

### Endpoint

```
POST http://<IP_DEL_DISPOSITIVO>:8080/api/sendsms
```

### Cabeceras requeridas

| Cabecera | Descripción |
|---|---|
| `Content-Type` | `application/json` |
| `x-api-key` | API Key configurada en la app |
(+ OPCIONALES)|Puedes colocar mas pero se omitiran

### Cuerpo de la petición (JSON)
Solo se ha probado con números del Perú(sin codigo-directo)
```json
{
  "to": "987654321",
  "message": "Hola desde SmsSender!",
  "sim_slot": 1
}
```

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `to` / `phone` | `string` | ✅ | Número de teléfono destino |
| `message` | `string` | ✅ | Texto del SMS |
| `sim_slot` | `integer` | ❌ | Ranura SIM (1, 2...). Si se omite, usa la predeterminada |
| `data_coding` | `integer` | ❌ | Codificación del mensaje |
| `status` | `boolean` | ❌ | Solicitar reporte de estado |

### Respuesta exitosa (HTTP 200)

```json
{
  "success": true,
  "message": "SMS enviado exitosamente",
  "messageId": "a1b2c3d4-...",
  "simUsed": "SIM 1"
}
```

### Respuestas de error

| Código | Motivo |
|---|---|
| `400` | Body vacío o número/mensaje inválido |
| `401` | API Key inválida o no proporcionada |
| `404` | Ruta no encontrada |
| `405` | Método HTTP no permitido (solo POST) |
| `500` | Error interno al enviar el SMS |

---

## 🔑 Permisos de Android

La aplicación requiere los siguientes permisos:
- En caso no se otorguen, para que funcione tendras que ir a los ajustes de aplicaciones de tu celualr y activar los permisos manualmente

| Permiso | Uso |
|---|---|
| `SEND_SMS` | Enviar mensajes SMS |
| `INTERNET` | Levantar el servidor HTTP local |
| `READ_PHONE_STATE` | Detectar SIMs activas |
| `FOREGROUND_SERVICE` | Mantener el servidor activo en segundo plano |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Tipo especial de servicio en primer plano |
| `POST_NOTIFICATIONS` | Mostrar notificación del servidor activo |

---

## ⚙️ Configuración por defecto (`gradle.properties`)

| Parámetro | Valor por defecto | Descripción |
|---|---|---|
| `APP_PORT` | `8080` | Puerto del servidor HTTP |
| `ENDPOINT` | `/api/sendsms` | Ruta del endpoint |
| `SMS_TIMEOUT_SECONDS` | `15` | Tiempo máximo de espera por confirmación SMS |
| `DEFAULT_SIM_SLOT` | `0` | SIM predeterminada (0 = sistema) |
| `MAX_LOG_LINES` | `500` | Máximo de líneas en el log de la app |

---

## 🧪 Ejemplo con cURL

```bash
curl -X POST http://192.168.1.100:8080/api/sendsms \
  -H "Content-Type: application/json" \
  -H "x-api-key: TU_API_KEY" \
  -d '{"to": "+34612345678", "message": "Test desde cURL"}'
```

---

## 📝 Notas

- El dispositivo y el cliente deben estar en la **misma red WiFi** (o conectados por red local).
- La `API Key` se genera automáticamente en el primer arranque y `puede cambiarse` desde la interfaz.
- El servidor se ejecuta como **Foreground Service** para evitar que el sistema operativo lo cierre.
- El log de actividad muestra en tiempo real las peticiones recibidas y el estado de cada SMS.

---

## 📄 Licencia

Este proyecto es de uso libre

By Aref 2026