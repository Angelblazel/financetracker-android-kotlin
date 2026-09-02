# Documentación Oficial del Proyecto: FinanceTracker 📱💰

Bienvenido a la documentación oficial de **FinanceTracker** (anteriormente *NotiGastos* / *GastoLocal AI*), una solución moderna y de vanguardia para la automatización, centralización y seguimiento inteligente de gastos en dispositivos Android.

Este documento describe de manera exhaustiva la estructura de archivos, el diseño arquitectónico, el funcionamiento de los componentes clave de Inteligencia Artificial (híbrido Local/Nube), la lógica de interceptación de notificaciones y la visualización de datos en tiempo real.

---

## 📌 1. Visión General & Propósito

**FinanceTracker** es una aplicación diseñada para simplificar el registro de gastos diarios eliminando la fricción de la inserción manual. Logra esto mediante:
1. **Interceptación en Tiempo Real**: Un servicio en segundo plano escucha las notificaciones de entidades bancarias o aplicaciones de pago.
2. **Procesamiento con IA Híbrida**: Analiza el texto de las notificaciones utilizando modelos avanzados en la nube (**Gemini API**) o de manera 100% local e interactiva en el dispositivo (**Gemma-2B-IT CPU**).
3. **Persistencia Segura**: Almacena los gastos procesados en una base de datos local SQLite utilizando **Room**, garantizando la privacidad del usuario.
4. **Manejo Inteligente de Cuotas**: Integra un sistema para estandarizar las compras diferidas en cuotas mensuales, previniendo la doble contabilización y permitiendo al usuario decidir si prefiere registrar la cuota individual o proyectar el monto total.
5. **Dashboard Analítico**: Ofrece gráficas visuales modernas (gráfico de dona y barras de tendencia semanal) construidas nativamente con Jetpack Compose.

---

## 📂 2. Estructura Completa del Proyecto

A continuación se detalla la organización de los directorios y archivos fuente principales del módulo de la aplicación (`/app`):

```text
/app
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── example
│   │   │           ├── ExpenseApp.kt             # Clase Application para inicializaciones globales
│   │   │           ├── MainActivity.kt           # Punto de entrada de la UI y setup inicial de permisos
│   │   │           │
│   │   │           ├── data                      # Capa de Acceso a Datos (Data Layer)
│   │   │           │   ├── local
│   │   │           │   │   ├── AppDatabase.kt    # Base de datos SQLite gestionada con Room
│   │   │           │   │   ├── ExpenseDao.kt     # Interfaz DAO con consultas SQL y reactivas (Flow)
│   │   │           │   │   └── LocalLlmManager.kt# Orquestador del LLM Gemma-2B local vía MediaPipe GenAI
│   │   │           │   │
│   │   │           │   ├── model
│   │   │           │   │   └── Expense.kt        # Entidad de dominio 'Expense' (Tabla de Base de Datos)
│   │   │           │   │
│   │   │           │   ├── remote
│   │   │           │   │   └── GeminiApi.kt      # Cliente REST / HTTP para realizar inferencias con Gemini en la Nube
│   │   │           │   │
│   │   │           │   └── repository
│   │   │           │       └── ExpenseRepository.kt # Repositorio único (Single Source of Truth) y lógica de cuotas
│   │   │           │
│   │   │           ├── service                   # Capa de Servicios de Android
│   │   │           │   └── ExpenseNotificationListenerService.kt # Escucha activa y parsing inicial de notificaciones
│   │   │           │
│   │   │           └── ui                        # Capa de Interfaz de Usuario (UI Layer)
│   │   │               ├── ExpenseScreen.kt      # Vistas Jetpack Compose (Dashboard, Analíticas, Logs, Ajustes)
│   │   │               ├── ExpenseViewModel.kt   # ViewModel con el estado reactivo (StateFlow) de la UI
│   │   │               └── theme                 # Centralización de colores y tipografía de Material Design 3
│   │   │
│   │   ├── res                                   # Recursos de Android (Assets estáticos)
│   │   │   └── values
│   │   │       └── strings.xml                   # Configuración del nombre de la app launcher (FinanceTracker)
│   │   └── AndroidManifest.xml                   # Registro de servicios, permisos del sistema y actividades
│   │
│   └── test                                      # Entorno de pruebas unitarias y Robolectric
│
├── build.gradle.kts                              # Configuración de compilación del módulo app
└── metadata.json                                 # Sincronización de metadatos de la plataforma AI Studio
```

---

## 🏛️ 3. Arquitectura del Software

**FinanceTracker** sigue los principios de la **Arquitectura Limpia (Clean Architecture)** y el patrón de diseño **MVVM (Model-View-ViewModel)** de forma estrictamente reactiva:

```text
 ┌───────────────────────────────────────────────────────────┐
 │                       CAPA DE UI                          │
 │  [Jetpack Compose Screens]  ◄───►  [ExpenseViewModel]    │
 └─────────────────────────┬───────────────────▲─────────────┘
                           │                   │
                           │ Invoca            │ Emite Estados
                           ▼                   │ (StateFlow)
 ┌─────────────────────────────────────────────┴─────────────┐
 │                    CAPA DE DOMINIO / REPOSITORIO          │
 │                    [ExpenseRepository]                    │
 └─────────────────────────┬───────────────────▲─────────────┘
                           │                   │
             Decide Origen │                   │ Retorna Datos
             o Persiste    ▼                   │
 ┌─────────────────────────────────────────────┴─────────────┐
 │                     CAPA DE DATOS (DATA)                  │
 │  [AppDatabase / Room]      [LocalLlmManager (Gemma)]      │
 │  [GeminiApi (Nube)]        [Notification Listener]        │
 └───────────────────────────────────────────────────────────┘
```

### Principales directrices de diseño implementadas:
*   **Separación de Responsabilidades**: Las vistas no conocen cómo se procesan ni de dónde se descargan los datos. Únicamente reaccionan a los cambios de estado expuestos por el `ViewModel`.
*   **Single Source of Truth**: El `ExpenseRepository` decide si consume el LLM en la nube o el local, estandariza los montos de cuotas, y guarda los resultados en `Room`. Toda la UI lee la base de datos a través de flujos reactivos (`Flow<List<Expense>>`), lo que asegura actualización instantánea tras cada inserción.
*   **Inyección de Dependencias Simple**: Se utiliza inyección por constructor para mantener el desacoplamiento y facilitar el testing sin introducir la sobrecarga de frameworks pesados de DI.

---

## ⚙️ 4. Componentes Clave & Flujo de Datos

### A. Interceptor de Notificaciones (`ExpenseNotificationListenerService`)
Es un servicio de Android que se ejecuta continuamente en segundo plano. Cuando el sistema operativo recibe una notificación (ej: de una aplicación bancaria como Nequi, Daviplata, Bancolombia, BBVA, etc.):
1. El servicio intercepta la notificación y filtra el remitente y contenido.
2. Si la notificación contiene palabras clave de transacciones (ej: *compra*, *retiro*, *pago*, *recepción*, *transferencia*), crea un registro de gasto temporal en estado **Pendiente**.
3. Desencadena inmediatamente una tarea asíncrona mediante Coroutines en el `ExpenseRepository` para procesar el texto sin bloquear el hilo principal.

### B. Base de Datos Local (`Room`)
El esquema de datos central es el modelo `Expense`:
*   **Campos Clave**: `id` (Auto-incrementable), `amount` (Monto), `merchant` (Comercio), `category` (Categoría asignada automáticamente), `currency` (Símbolo de divisa), `timestamp` (Fecha y hora del evento), `originalText` (Texto bruto de la notificación interceptada), `isPending` (Estado de análisis), y `parseError` (Para depuración).
*   **Reactividad**: El `ExpenseDao` expone un flujo `Flow<List<Expense>>` que emite una nueva lista cada vez que hay inserciones, actualizaciones o eliminaciones, provocando una recomposición limpia en la UI de Jetpack Compose.

### C. Motor Dual de Inteligencia Artificial

#### 🌌 1. Modo Nube (Gemini API)
*   **Funcionamiento**: Envía el texto original a través de llamadas seguras REST a los servidores de Gemini.
*   **Estructuración**: Utiliza un prompt altamente especializado que obliga al modelo a responder estrictamente en formato JSON válido, lo que facilita un parseo directo sin errores.

#### 📴 2. Modo Local (Gemma-2B CPU - MediaPipe)
*   **Funcionamiento**: FinanceTracker incorpora soporte para ejecutar inferencia de IA **100% offline y de manera privada** en el chip del dispositivo del usuario.
*   **MediaPipe LLM Inference**: Se apoya en la biblioteca GenAI de Google MediaPipe para cargar y ejecutar el modelo cuantizado `gemma-2b-it-cpu-int4.bin`.
*   **Gestor de Descarga Integrado**: La UI incluye una tarjeta de descarga asíncrona interactiva. Esta tarjeta se conecta directamente a HuggingFace, maneja redirecciones automáticas de red, y almacena el archivo de 1.35 GB en el almacenamiento interno privado de la aplicación, mostrando una barra de progreso detallada y control de estados (Idle, Downloading, Success, Error).

---

## 💳 5. Módulo de Estandarización de Cuotas

Para las compras diferidas (ej: *"Compra de USD 300 en Amazon a 12 cuotas de USD 25"*), las aplicaciones bancarias o pasarelas de pago suelen notificar cobros recurrentes de manera mensual. Esto solía causar **doble contabilización** o registros distorsionados en las apps financieras tradicionales.

FinanceTracker resuelve este problema mediante un motor de estandarización inteligente basado en expresiones regulares y procesamiento semántico:

### Modos de Configuración en Ajustes:
El usuario puede cambiar el comportamiento del manejo de cuotas desde el panel de ajustes de la aplicación, guardándose de manera persistente en `SharedPreferences`:

1.  **Cuota Individual (`individual`) [Recomendado]**:
    *   **Lógica**: Extrae el valor unitario de la cuota (ej: $10.000 COP) y registra únicamente ese valor. Previene que en los meses siguientes se dupliquen gastos inflados, asumiendo que cada notificación mensual representará el pago correspondiente a ese periodo.
2.  **Monto Total (`total`)**:
    *   **Lógica**: Multiplica el número de cuotas por el valor de cada una para calcular el impacto financiero total de forma inmediata (ej: 3 cuotas de $10.000 COP = $30.000 COP registrados instantáneamente). Ideal para usuarios que prefieren contabilizar toda la deuda el día 1.

---

## 📊 6. Capa de UI & Visualización Dinámica

La interfaz gráfica de **FinanceTracker** está construida en su totalidad sobre **Jetpack Compose** aplicando los lineamientos visuales modernos de **Material Design 3**:

*   **Paleta de Colores Moderna**: Uso dinámico de contenedores tonales y un elegante tema oscuro con contrastes optimizados para evitar la fatiga visual.
*   **Gráfico de Dona (Donut Chart)**:
    *   Dibujado mediante un componente `Canvas` nativo usando arcos concéntricos con `StrokeCap.Round`.
    *   Asigna colores temáticos únicos según la categoría del gasto (*Food, Transport, Shopping, Entertainment, Utilities, Services, Others*).
    *   Muestra el monto acumulado total y la divisa seleccionada en el centro geométrico del gráfico.
*   **Gráfico de Tendencia Semanal (SpendingTrendChart)**:
    *   Visualiza barras verticales proporcionales con bordes redondeados representando los gastos de los últimos 7 días.
    *   Implementa detección inteligente de días con $0 de gasto, dibujando un indicador sutil en lugar de un vacío para mantener la consistencia estética.
*   **Filtro Multidivisa**:
    *   Si existen gastos registrados en diferentes monedas (ej: USD y COP), la interfaz de estadísticas habilita chips de filtrado dinámico. Al pulsarlos, toda la distribución y gráficos se recalculan instantáneamente para la divisa seleccionada.

---

## 🗝️ 7. Seguridad, Permisos y Variables de Entorno

### Permisos Requeridos (Declarados en el Manifiesto):
*   `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`: Requerido para que la aplicación pueda escuchar las notificaciones entrantes de transacciones de otras apps.
*   `android.permission.INTERNET`: Necesario para descargar el modelo Gemma-2B local y para comunicarse con la API de Gemini en la nube.

### Gestión de Secretos:
Las credenciales confidenciales, como la API Key de Gemini, se administran de forma segura a través del sistema de Gradle y variables de entorno definidas en el archivo `.env`. El código fuente accede a estos valores de manera limpia a través de `BuildConfig` generado automáticamente, mitigando cualquier riesgo de filtración de claves en los repositorios de código público.
