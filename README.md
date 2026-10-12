# Portal Puntano 5.0

Rediseño de **Portal Puntano 4.0** como app Android nativa con estilo visual **High-Tech Corporativo · utopía futurista**.

| Capa | Lenguaje |
|---|---|
| Datos, estructura y arquitectura | Kotlin |
| Interfaz | HTML + CSS + JavaScript (`app/src/main/assets/ui`) |

## Cómo abrirlo
1. Abrir la carpeta en Android Studio (JDK 17).
2. Sincronizar Gradle y ejecutar el módulo `app`.
3. La app se inicia en modo demo (`DEMO_MODE=true` en `app/build.gradle.kts`).

## Estado
Proyecto inicializado. La validación de endpoints, DTOs y operaciones de algunos módulos requiere pruebas con los servicios reales.