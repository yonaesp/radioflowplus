# RadioAndroid - Contexto del Proyecto

**Última actualización:** 2026-01-13 (Sesión de Optimización y Limpieza)

## 🌐 Idioma
**SIEMPRE comunicar en ESPAÑOL con el usuario.**

---

## 🚀 Estado Actual del Proyecto: "RadioFlow+" Simplificado

### ✅ Logros Recientes (Enero 2026)
1. **Instant Skip (<1s) IMPLEMENTADO Y VERIFICADO:**
   - **Problema:** ExoPlayer tardaba 8s en reaccionar a errores definitivos porque envolvía las excepciones (ej: `ConnectException` dentro de `HttpDataSourceException` dentro de `ExoPlaybackException`), burlando los chequeos simples.
   - **Solución:** Implementada **inspección recursiva de causas raíz** en `RadioLoadErrorHandlingPolicy.kt`.
   - **Resultado:** Errores como `ConnectException` o `UnknownHostException` ahora se detectan inmediatamente como FATALES y detienen los reintentos al instante.
2. **Limpieza Profunda ("Code Diet"):**
   - Eliminado **TODO** el subsistema de "Info de Canción" (Metadata, ArtworkFetcher, IcyInfo).
   - Eliminado `ArtworkFetcher.kt` y librerías asociadas.
   - Eliminados toggles de configuración obsoletos en `AppPreferences` y UI.
   - `PlayerBar` simplificado para mostrar solo Logo + Nombre + Estado/Género.
3. **Optimización de Tests:**
   - La emisora de prueba "TEST - Enlace Roto" ahora apunta a `http://127.0.0.1:54321`.
   - Esto garantiza un error `ConnectException` (Connection Refused) **instantáneo** en cualquier dispositivo, sin depender de timeouts de DNS.

---

## 🏗️ Build & Deploy

### Configuración
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

### Comandos Clave (Windows PowerShell)
```powershell
# Build Limpia (Producción - RECOMENDADO)
./gradlew --stop; Remove-Item -Path "app/build" -Recurse -Force -ErrorAction SilentlyContinue; ./gradlew clean assembleRelease

# Verificar APK
ls -l app/build/outputs/apk/release/app-release.apk

# Instalar (Solo bajo demanda)
adb install -r "app/build/outputs/apk/release/app-release.apk"
```

---

## 🧠 Arquitectura de Error Handling (RadioFlow+)
La app usa una estrategia de 3 capas para garantizar reproducción continua:

1. **Capa 1: RadioLoadErrorHandlingPolicy (ExoPlayer Level)**
   - Intercepta errores de carga ANTES de que ExoPlayer decida reintentar.
   - **Unwrapping:** Busca recursivamente `ConnectException`, `UnknownHostException`, errors HTTP 4xx/5xx.
   - Si detecta FATAL -> Devuelve `C.TIME_UNSET` (No Retry).
   - Resultado: ExoPlayer falla inmediatamente y llama a `onPlayerError`.

2. **Capa 2: RadioMediaService (Service Level)**
   - Recibe `onPlayerError`.
   - Si es error FATAL (confirmado por código o mensaje): **SKIP STATION** (<1s).
   - Si es error AMBIGUO: Usa `SmartRetryManager` (reintento progresivo).

3. **Capa 3: Watchdog & User Feedback**
   - Notificaciones claras.
   - Toast informativos.

---

## 🎨 Gestión de Logos
*(Consolidado de .cursorrules)*

- **Originals:** `logos/originals/` (PNGs sagrados).
- **Procesados:** `app/src/main/res/drawable/logo_*.webp`.
- **Regla de Oro:** NUNCA usar eliminación automática de fondo. Buscar logos transparentes nativos.
- **Herramienta:** `python scripts/optimize_logos.py`.

---

## 📁 Archivos Clave para Mantenimiento

- **Service:** `app/src/main/java/com/radioandroid/service/RadioMediaService.kt` (Núcleo lógico).
- **Player:** `app/src/main/java/com/radioandroid/player/RadioLoadErrorHandlingPolicy.kt` (Lógica de reintentos y detección de errores).
- **Data:** `app/src/main/java/com/radioandroid/data/RadioStations.kt` (Lista de emisoras).
- **UI:** `app/src/main/java/com/radioandroid/ui/components/PlayerBar.kt` (Barra de reproducción).

---

## 📝 PARA LA SIGUIENTE SESIÓN (RESUME)

Cuando vuelvas a abrir el proyecto:

1. **Estado:** APK generado y limpio (`app-release.apk` ~5.8MB). Codebase optimizado.
2. **Pendiente:** Verificar si el usuario desea añadir más emisoras o refinar la UI visualmente.
3. **Verificación:** Si el usuario reporta problemas de skip, verificar `RadioLoadErrorHandlingPolicy.kt` -> método `isFatal`.

**¡LISTO PARA VOLAR!** 🚀
