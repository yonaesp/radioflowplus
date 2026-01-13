---
description: Verificar URLs de streaming de radio antes de añadirlas
---

# Verificación de URLs de Radio Streaming

## Regla Crítica
**NUNCA** añadir URLs de radio sin verificar que son streams EN VIVO (infinite duration), no archivos de audio finitos.

## Fuentes de URLs NO CONFIABLES (evitar):
- `stream.zeno.fm/*` - Sirve clips de audio finitos, no radio en vivo
- `cdn.instream.audio/*` - Sirve archivos de audio pregrabados
- `*.mexside.net/*` - Calidad variable, a menudo clips finitos
- Cualquier URL de terceros genéricos que no sea el CDN oficial de la emisora

## Fuentes de URLs CONFIABLES:
- `playerservices.streamtheworld.com` - Infraestructura oficial de iHeartMedia
- `flumotion.com` / `cope.stream.flumotion.com` - CDN oficial de COPE/Rock FM
- `rtve.es` / `rtvelivestream.rtve.es` - Radio Televisión Española oficial
- `akamaized.net` - CDN de Akamai usado por BBC y otros
- `ondacero.es` / `atresmedia*` - Atresmedia oficial
- URLs directas del dominio oficial de la emisora

## Cómo verificar una URL:
1. **Buscar la URL oficial**: Siempre buscar "NOMBRE_EMISORA stream URL oficial" 
2. **Verificar el dominio**: ¿Es el CDN oficial de la emisora o su empresa matriz?
3. **Probar duración**: Si el stream termina después de segundos/minutos, es un archivo finito
4. **Preferir MP3/AAC sobre HLS**: Los streams MP3/AAC directos son más estables que m3u8

## Señales de URL problemática:
- Stream que termina después de 5-30 segundos
- Dominio genérico sin relación con la emisora
- Error `STATE_ENDED` inmediatamente después de conexión
- La app salta automáticamente a la siguiente emisora

## Acción cuando se detecta problema:
1. Comentar la emisora con nota explicativa
2. Buscar URL oficial alternativa
3. Si no hay URL pública, documentar en código que no hay stream disponible
