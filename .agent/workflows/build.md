---
description: Instrucciones para compilar la app Android correctamente
---

# Build Process Workflow

## Build Normal
```powershell
// turbo
./gradlew assembleRelease
```

## Reglas

1. Ejecuta el comando UNA sola vez
2. Si ves "BUILD SUCCESSFUL" en el output, el build terminó
3. **NO sigas esperando** ni haciendo "command_status" en bucle
4. **NO ejecutar** `./gradlew --stop` salvo error específico del daemon
5. **NO hacer clean** salvo errores de caché corrupta
6. **NO reintentar** después de un build exitoso
7. Si el comando tarda >60s sin output nuevo, verifica si existe la APK
8. APK lista en: `app/build/outputs/apk/release/app-release.apk`

## Cuándo hacer clean
Solo si hay errores de compilación relacionados con caché corrupta:
```powershell
./gradlew clean assembleRelease
```

## Cuándo hacer --stop
Solo si hay errores "Daemon is busy" o "Could not connect to daemon":
```powershell
./gradlew --stop
./gradlew assembleRelease
```
