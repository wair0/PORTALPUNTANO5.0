# ESTADO TÉCNICO Y CONTRATOS — PORTAL PUNTANO 5.0

## Alcance

Este documento registra el estado comprobado del repositorio y los contratos entre la interfaz WebView, el puente Android, los repositorios de dominio y la API. No declara como operativos servicios que todavía no están integrados o validados.

## Arquitectura actual

- **Interfaz:** HTML, CSS y JavaScript local en `app/src/main/assets/ui/`.
- **Puente:** `PortalBridge` / `BridgeRouter`; la interfaz solicita operaciones mediante rutas nombradas.
- **Dominio:** modelos serializables e interfaces `AuthRepository` y `PortalRepository`.
- **Datos:** `PortalRepositoryImpl` y `AuthRepositoryImpl`, con selección explícita entre modo demo y modo conectado.
- **Red:** `NetworkModule` y `PortalApi`, con Retrofit y configuración de sesión.

## Contrato de rutas del puente

| Ruta | Comportamiento observado | Estado |
|---|---|---|
| `app.status` | Informa si está habilitado el modo demo | Implementado |
| `auth.session` | Devuelve la sesión actual | Implementado |
| `auth.login` / `auth.logout` | Inicio y cierre de sesión | Implementado; requiere validación contra el servicio oficial |
| `auth.scanDni` | Solicita escaneo de DNI | Implementado; requiere prueba en dispositivo |
| `home.summary` | Resume perfil, turno y trámites | Depende de turnos y trámites |
| `health.overview` | En modo conectado falla explícitamente porque el servicio aún no está integrado | Pendiente |
| `shifts.list` | Consulta turnos | Implementado en cliente; contrato remoto por validar |
| `procedures.list` | Consulta trámites | Implementado en cliente; contrato remoto por validar |
| `payslips.list` | Consulta liquidaciones | Implementado en cliente; contrato remoto por validar |
| `credentials.list` | Consulta credenciales de conducir y salud | Implementado con manejo de errores; validar API real |
| `education.students` | Consulta dependientes/estudiantes | Implementado en cliente; contrato remoto por validar |
| `judicial.list` | Consulta expedientes | Implementado en cliente; contrato remoto por validar |
| `civil.certificates` | Consulta certificados | Implementado en cliente; contrato remoto por validar |
| `files.download` | Devuelve error explícito indicando que la descarga aún no está disponible | Pendiente |

## Preparación de integraciones oficiales (fase 5)

- Las URLs de Portal, Salud, Receta Digital y Educación ya no se fijan como endpoints HTTP dentro del código fuente: se leen de propiedades Gradle o variables de entorno.
- En modo conectado, `PORTAL_BASE_URL` debe estar definido y usar HTTPS; si falta o es inseguro, la app falla con un mensaje explícito antes de crear Retrofit.
- La compilación de demostración mantiene `DEMO_MODE=true`; no debe interpretarse como una integración oficial operativa.
- **Pendiente de información del proveedor:** confirmar las URLs oficiales HTTPS, rutas, esquema de autenticación, DTO y ejemplos de respuesta para cada servicio. No se sustituyen por endpoints adivinados.

## Sesión y seguridad

- La sesión se conserva en `EncryptedSharedPreferences`, cifrada mediante una `MasterKey`.
- El inicio de sesión persiste token, documento y nombre en una sola edición de preferencias; los tokens vacíos se rechazan.
- El cierre de sesión borra los valores locales. Una respuesta HTTP 401 invalida la sesión para no reutilizar credenciales caducadas.
- El cliente de red exige HTTPS en modo conectado y no adjunta el token previo a la solicitud de login.
- Estas medidas no sustituyen las pruebas contra la API oficial ni una verificación en dispositivo real.

## Comportamiento esperado sin conexión

- **Solo la presentación visual es local:** HTML, CSS, JavaScript, iconos y animaciones se sirven desde los recursos empaquetados de la app mediante `WebViewAssetLoader`.
- **Los datos no se consideran offline:** perfil, turnos, trámites, liquidaciones y demás información deben venir de la sesión y de los servicios correspondientes; sin conexión, la app debe informar del error y no presentar datos ficticios como actuales.
- **No confundir caché visual con disponibilidad de datos:** poder abrir la interfaz sin Internet no implica que las consultas, el inicio de sesión ni las operaciones remotas funcionen offline.
- **Modo demo:** los datos ficticios solo son aceptables cuando la app indica claramente que está en modo demostración.

## Reglas obligatorias

1. No mostrar datos ficticios como si fueran reales en modo conectado.
2. No convertir errores de red, autenticación o cancelación en respuestas de éxito vacías.
3. Propagar `CancellationException` para respetar la cancelación de corutinas.
4. Mantener las llamadas de red en la capa de datos; la interfaz usa rutas del puente y no accede directamente a servicios externos.
5. No activar el modo conectado por defecto ni declarar una API real lista hasta validar URL, autenticación, DTO y respuestas con el proveedor oficial.
6. Las rutas que todavía no tienen implementación deben mostrar un estado pendiente explícito, no una simulación silenciosa.

## Criterios de cierre de la fase 2

- [x] Identificar capas y puntos de entrada actuales.
- [x] Enumerar rutas públicas del puente y su estado observado.
- [x] Identificar módulos que fallan explícitamente por falta de integración.
- [ ] Validar contratos de API con documentación/proveedor oficial.
- [ ] Añadir pruebas de contrato para respuestas válidas y errores HTTP; la invalidación local ante HTTP 401 está cubierta por pruebas estáticas.
- [x] Verificar automáticamente que las rutas de pantalla y las llamadas visibles de la UI existan en el puente nativo; las descargas siguen marcadas como pendientes.

## Próximos pasos

1. Completar la fase 2 con pruebas de contrato y revisión de rutas.
2. Cerrar la fase 3 comprobando carga local de la presentación visual y navegación; verificar por separado que las consultas de datos requieren conectividad y muestran errores explícitos cuando no la hay.
3. Revisar seguridad de sesión, WebView y políticas de contenido en la fase 4.
4. Completar la fase 5 al recibir y validar contratos oficiales: URLs HTTPS, autenticación, rutas, DTO y respuestas de los servicios.
5. Probar módulos y regresiones en la fase 6.
6. Cerrar la fase 7 con CI verde, APK descargable directamente y pruebas en dispositivo.
