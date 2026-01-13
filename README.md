# RadioFlow+ 📻

**La app de radio más completa para Android** — Control por voz, alarma inteligente, Android Auto y experiencia premium.

[![Android](https://img.shields.io/badge/Android-7.0%2B-green.svg)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple.svg)](https://kotlinlang.org)
[![ExoPlayer](https://img.shields.io/badge/ExoPlayer-Media3-blue.svg)](https://developer.android.com/media/media3)

---

## ✨ Funciones Principales

### 🎙️ Control por Voz (Gratis)
Controla la radio sin tocar el móvil. Perfecto para conducir.

| Comando | Acción |
|---------|--------|
| "Hey Google, pon Los 40" | Reproduce la emisora |
| "Hey Google, pausa la radio" | Pausa/reanuda |
| "Hey Google, siguiente emisora" | Cambia emisora |
| "Hey Google, sube el volumen" | Ajusta volumen |

> Compatible con **Google Assistant** y **Gemini**

---

### 💤 Sleep Timer (Gratis)
Apaga la radio automáticamente después de un tiempo. Ideal para dormir.

- ⏱️ Elige 15, 30 o 60 minutos
- 📴 Se apaga sola al terminar
- 🌙 Perfecto para escuchar antes de dormir

---

### 🎨 Personalización (Gratis)
Adapta la app a tu estilo.

- 🌙 **Tema oscuro**: Ideal para usar de noche
- ☀️ **Tema claro**: Mejor visibilidad al sol
- 📱 **Automático**: Cambia según el sistema

---

### 🚗 Auto-Reanudación (Gratis)
La radio se reanuda automáticamente al conectar Bluetooth o Android Auto.

- Arranca el coche → La radio empieza sola
- Conecta auriculares → Reanuda donde lo dejaste
- Sin necesidad de tocar nada

---

### 🧭 Navegación Inteligente (Gratis)
Personaliza cómo cambias entre emisoras con los botones ⏮️ ⏭️.

| Opción | Descripción |
|--------|-------------|
| Por País | Solo emisoras del mismo país |
| Por Categoría | Solo del mismo estilo musical |
| Agrupar | Ver emisoras organizadas por tipo |

---

### 📶 Gestión de Conexión (Gratis)
La app detecta problemas de red y se recupera automáticamente.

- 📶 Aviso "Señal baja" cuando hay problemas
- 🔄 Reanuda sola cuando vuelve la conexión
- 🔋 No consume batería esperando

---

### ⚡ Auto-Skip Inteligente (Gratis)
Si una emisora no funciona, salta automáticamente a la siguiente.

| Situación | Comportamiento |
|-----------|----------------|
| URL rota (404) | Salta en <1 segundo |
| Servidor lento | Espera hasta 8 segundos |
| Sin conexión | Espera a que vuelva la red |

---

### 🔊 Audio Focus Inteligente (Gratis)
La radio se adapta cuando otras apps reproducen audio.

- 🔉 GPS/Notificaciones → Baja volumen 80%
- ⏸️ Videos/Llamadas → Pausa completamente
- ▶️ Vuelve al directo automáticamente

---

### ❤️ Favoritos (Gratis con límite)
Guarda tus emisoras favoritas para acceder más rápido.

- Pulsa ❤️ en cualquier emisora
- Favoritos aparecen arriba de la lista
- **Gratis**: Hasta 5 favoritos

---

## ⭐ Funciones Premium

> Desbloquea todo el potencial de RadioFlow+ con la suscripción Premium

### ⏰ Radio Alarma ⭐
Despierta con tu emisora favorita.

- 🌅 Elige hora y emisora
- 🔊 Fade-in suave del volumen
- 📱 Funciona con pantalla bloqueada
- 🔴 Botón grande para apagar

### ⏸️ Time-Shift ⭐
Pausa la radio en directo y continúa donde lo dejaste.

- ⏸️ Pausa mientras atiendes algo
- ▶️ Continúa exactamente donde paraste
- ⏱️ Hasta 2 minutos de buffer

### 🌟 Navegar Solo Favoritos ⭐
Los botones ⏮️ ⏭️ solo cambian entre tus favoritos.

- Salta directo a tus emisoras preferidas
- Ignora las demás emisoras
- Acceso ultra-rápido

### ❤️ Favoritos Ilimitados ⭐
Sin límite de 5 favoritos.

- Guarda todas las emisoras que quieras
- Organiza todo tu contenido
- Acceso rápido a más emisoras

### 📺 Chromecast ⭐
Transmite a cualquier dispositivo Cast.

- 📺 Chromecast, Google Home, Smart TVs
- 🔊 Audio en tus altavoces
- 📱 Controla desde el móvil

---

## 🚗 Android Auto

RadioFlow+ funciona perfectamente con Android Auto.

- 📻 Navega emisoras desde la pantalla del coche
- 🎙️ Usa comandos de voz para cambiar
- 🖼️ Logos visibles mientras conduces
- ⚙️ Los ajustes aplican igual

---

## 📻 Emisoras Disponibles

### 🇪🇸 España
| Emisora | Género |
|---------|--------|
| Los 40 Principales | Pop |
| Cadena SER | Noticias |
| COPE | Noticias |
| Onda Cero | Noticias |
| Rock FM | Rock |
| Cadena 100 | Pop |
| Europa FM | Pop |
| Kiss FM | Dance |
| Máxima FM | Dance |
| Radio Nacional | Variada |
| Radio 3 | Alternativa |
| *...y muchas más* | |

### 🇲🇽 México
| Emisora | Género |
|---------|--------|
| Los 40 México | Pop |
| EXA FM | Pop |
| La Mejor | Regional |
| MVS Noticias | Noticias |
| Alfa 91.3 | Pop |
| Beat 100.9 | Dance |
| Universal Stereo | Clásica |
| Horizonte 107.9 | Jazz |
| Opus 94.5 | Clásica |
| *...26 emisoras verificadas* | |

---

## 🔧 Características Técnicas

- **ExoPlayer Media3** optimizado para streaming en vivo
- **Buffering inteligente**: 1.5s inicio rápido, 60s buffer máximo
- **Soporte AAC/HLS/MP3** con configuración específica
- **Política de reintentos**: No reintenta errores definitivos
- **Timeout HTTP agresivo**: 2s para detectar URLs rotas

---

## 📱 Requisitos

- Android 7.0 (API 24) o superior
- Conexión a Internet
- Permisos de notificación (opcional)
- Permisos de alarma exacta (para Radio Alarma)

---

## 🛠️ Desarrollo

### Build Release
```bash
./gradlew assembleRelease
```

### Clean Build
```bash
./gradlew clean assembleRelease
```

### Debug Build
```bash
./gradlew assembleDebug
```

---

## 📄 Licencia

Copyright © 2026 RadioFlow+. Todos los derechos reservados.

---

<p align="center">
  Made with ❤️ in Spain
</p>
