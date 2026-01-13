---
description: Workflow para desarrollo, build e instalación de la app RadioAndroid
---

// turbo-all

## Configuración de entorno

El proyecto requiere JAVA_HOME configurado para Android Studio:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

## Build Debug APK (Clean + Assemble)

1. Limpiar y compilar el APK de debug:
```powershell
cd c:\Users\jonat\Documents\antigravity\RadioAndroid
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; ./gradlew clean assembleDebug --quiet
```

## Ruta del APK

El APK se genera en:
```
C:\Users\jonat\Documents\antigravity\RadioAndroid\app\build\outputs\apk\release\app-release.apk
```

## Instalación en emulador/dispositivo

2. Instalar en dispositivo conectado:
```powershell
adb install -r "C:\Users\jonat\Documents\antigravity\RadioAndroid\app\build\outputs\apk\release\app-release.apk"
```

## Notas

- Siempre usar `clean assembleRelease` para reconstruir completamente
- El APK queda en la ruta estándar de Gradle: `app\build\outputs\apk\release\`
- El workflow tiene `turbo-all` para auto-ejecutar todos los comandos
