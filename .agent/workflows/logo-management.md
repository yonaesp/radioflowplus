---
description: Gestión de logos de emisoras de radio - buscar, descargar y procesar logos
---

# Gestión de Logos de Emisoras

## REGLA FUNDAMENTAL
NUNCA usar scripts de eliminación de fondo automática (rembg, detección de color, etc.). Siempre obtener logos que YA tengan transparencia correcta desde internet.

## ESTRUCTURA DE ARCHIVOS

```
logos/
├── originals/           # PNGs originales descargados (NUNCA borrar)
├── processed/           # WebPs procesados (backup)
└── sources.md           # Fuentes documentadas

app/src/main/res/drawable/
└── logo_*.webp          # Logos finales usados por la app
```

## PROCESO PARA AÑADIR NUEVA EMISORA

### PASO 1: Buscar logo transparente

Buscar en este orden de prioridad (de más rápido a más lento):

1. **Seeklogo.com** (MUY RÁPIDO, sin captcha):
   - URL directa: https://seeklogo.com/free-vector-logos/[nombre-emisora]
   - Descarga directa PNG/SVG, sin registro

2. **Wikimedia Commons** (MUY RÁPIDO, alta calidad):
   - Buscar: "[nombre emisora] logo site:commons.wikimedia.org"
   - URL típica: https://commons.wikimedia.org/wiki/File:[Nombre]_logo.svg

3. **Brandfetch.com** (RÁPIDO, siempre transparente):
   - URL: https://brandfetch.com/[dominio-de-la-emisora]

4. **Worldvectorlogo.com** (RÁPIDO)

5. **Wikipedia** (artículo de la emisora)

6. **Google Images con filtro** (último recurso):
   - "[nombre emisora] logo PNG transparent"
   - Herramientas → Color → Transparente

### PASO 2: Verificar calidad antes de descargar

- ✅ Fondo de cuadros visible (transparencia real)
- ✅ Resolución mínima 400x400px (ideal 512+)
- ✅ Logo oficial actual
- ✅ Formato PNG o SVG

### PASO 3: Regla especial para logos con recuadro/marco

Si el logo oficial tiene recuadro (negro, azul, rojo, etc.), ESE RECUADRO ES PARTE DEL LOGO y debe respetarse.

Ejemplos:
- logo_ser.png → Recuadro azul = parte del logo
- logo_rockfm.png → Recuadro negro = parte del logo

### PASO 4: Guardar y documentar

1. Guardar PNG en: `logos/originals/logo_[nombreemisora].png`
2. Documentar en `logos/sources.md`

### PASO 5: Convertir a WebP

```bash
python scripts/optimize_logos.py --input logos/originals/logo_nombreemisora.png --output app/src/main/res/drawable/logo_nombreemisora.webp
```

### PASO 6: Añadir a RadioStations.kt

```kotlin
RadioStation(
    id = [siguiente_id],
    name = "Nombre Emisora",
    streamUrl = "https://...",
    logoResId = R.drawable.logo_nombreemisora,
    genre = "...",
    country = "..."
)
```

## QUÉ HACER SI NO EXISTE VERSIÓN TRANSPARENTE

1. Descargar la mejor calidad disponible (aunque tenga fondo)
2. Guardar en logos/originals/ igualmente
3. Documentar en sources.md que no tiene transparencia
4. El logo se mostrará con su fondo original

## PROHIBIDO

- ❌ Usar rembg o cualquier IA de segmentación
- ❌ Usar scripts de detección/eliminación de color de fondo
- ❌ Eliminar recuadros que son parte del diseño oficial
- ❌ Borrar la carpeta logos/originals/

## TIEMPOS ESPERADOS

- Logo encontrado en Seeklogo/Wikimedia: <1 minuto
- Logo no encontrado: Informar y continuar, no perder más de 5 minutos por logo

## VALIDACIÓN

```bash
python scripts/validate_logos.py --input-dir app/src/main/res/drawable/
```

Verificar en validation_report.html que se ven correctos sobre fondo claro Y oscuro.

## FUENTES ADICIONALES DESCUBIERTAS

### Emisoras Mexicanas:
- **Zeno.fm**: Logos de estaciones que transmiten ahí (ej: Alfa 91.3)
  - Patrón: `https://proxy.zeno.fm/content/stations/[uuid]/image/`
- **Grupo Radiorama (GCS)**: Para Sabrosita y estaciones del grupo
  - Patrón: `https://storage.googleapis.com/nrm-web/[emisora]/`
- **iHeart Radio**: Logos de alta calidad para estaciones asociadas
  - Patrón: `https://i.iheart.com/v3/re/new_assets/[id]`
- **Logopedia (Fandom)**: Historia completa de logos por call sign
  - URL: `https://logos.fandom.com/wiki/[CALL_SIGN]`

### Emisoras Internacionales:
- **TuneIn**: `https://cdn-profiles.tunein.com/[id]/images/logoq.png`
- **Radio.net**: Logos de buena calidad

## LIMITACIONES TÉCNICAS (Windows)

### Conversión SVG a PNG:
- **cairosvg** requiere librería nativa Cairo (no disponible fácil en Windows)
- **svglib + reportlab** también depende de Cairo
- **Solución**: Preferir descargar PNG directamente o usar herramientas online

### Alternativas para SVG:
1. Buscar versión PNG en la misma fuente
2. Usar navegador para capturar screenshot del SVG
3. Inkscape CLI: `inkscape input.svg --export-png=output.png --export-width=512`
4. Cloudconvert o similar (online)
