# Portal Puntano 5.0

Evolución de Portal Puntano 4.0: aplicación Android nativa con interfaz HTML/CSS integrada en WebView y capa de negocio en Kotlin.

## Estado del repositorio

- Rama principal: `main`.
- Android: Kotlin, Gradle 8.9, JDK 17, minSdk 24.
- Interfaz: HTML/CSS/JavaScript local, sin dependencias de CDN.
- El modo de demostración está habilitado por defecto (`DEMO_MODE=true`); no representa información real.
- El flujo de GitHub Actions compila el APK debug y publica directamente el archivo APK como artefacto, sin crear un ZIP adicional dentro del artefacto.

## Ejecutar

1. Abrir el repositorio en Android Studio con JDK 17.
2. Sincronizar Gradle y ejecutar el módulo `app`.
3. Para la compilación en CI, revisar la pestaña Actions del repositorio.
4. La vista previa autónoma se genera con `python3 tools/build_preview.py` y requiere `tools/preview_template.html`.

## Arquitectura

- `domain/model`: modelos serializables usados como contrato JSON entre Kotlin y la interfaz.
- `domain/repository`: interfaces de autenticación y servicios.
- `data/remote`: Retrofit, OkHttp y DTOs de API.
- `data/local`: almacenamiento cifrado de sesión.
- `data/demo`: datos ficticios para modo demo.
- `ui/web`: puente JavaScript/Kotlin y control de navegación del WebView.
- `scanner`: lectura del código PDF417 del DNI con Google Code Scanner.
- `app/src/main/assets/ui`: pantallas y estilos de la interfaz.

## Plan de trabajo por fases

1. **Base reproducible:** estructura Gradle, compilación CI y publicación directa del APK.
2. **Integración de interfaz:** asegurar que HTML, CSS, JavaScript e iconos se cargan desde assets locales y que la navegación funciona.
3. **Autenticación y sesión:** validar el contrato real de inicio de sesión, persistencia cifrada, expiración y cierre de sesión.
4. **Servicios:** comprobar endpoints y DTOs reales para turnos, trámites, recibos, credenciales, educación, expedientes y registro civil.
5. **Funciones pendientes:** descargas PDF, solicitudes de turno/trámite y recuperación de contraseña.
6. **Calidad y entrega:** pruebas en dispositivo, accesibilidad, seguridad de red y validación del APK publicado.

## Limitaciones conocidas

- Los endpoints y campos DTO deben contrastarse con respuestas reales de los servidores; se han inferido de la app anterior y no deben considerarse validados.
- La información médica permanece en modo demo hasta confirmar sus endpoints.
- La descarga de archivos todavía no está implementada en el puente nativo.
- El modo demo debe desactivarse antes de una distribución conectada a servicios reales.
- No se incluye `gradlew` ni `gradle-wrapper.jar`; CI instala Gradle 8.9 y Android Studio puede sincronizar el proyecto.
