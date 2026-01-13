# Logo Sources Documentation

This file documents where each logo was obtained.

## How to Add New Logos

1. **Find transparent logo** using these sources (in priority order):
   - Wikimedia Commons: `[station] logo site:commons.wikimedia.org`
   - Wikipedia: Look in the infobox of the station's page
   - Google Images: Tools → Color → Transparent
   - Brandfetch: https://brandfetch.com/
   - Seeklogo: https://seeklogo.com/

2. **Verify transparency**: Open in image viewer with checkered background

3. **Save to** `logos/originals/logo_stationname.png`

4. **Optimize**: 
   ```bash
   python scripts/optimize_logos.py --input logos/originals/logo_stationname.png --output app/src/main/res/drawable/logo_stationname.webp
   ```

5. **Add to RadioStations.kt**:
   ```kotlin
   logoResId = R.drawable.logo_stationname
   ```

6. **Document below** with source URL

---

## Spain

| Logo | Source | URL | Date |
|------|--------|-----|------|
| logo_los40 | Original | - | 2025-01 |
| logo_ser | Original | - | 2025-01 |
| logo_cope | Original | - | 2025-01 |
| logo_ondacero | Original | - | 2025-01 |
| logo_europafm | Original | - | 2025-01 |
| logo_rockfm | Original | - | 2025-01 |
| logo_cadenadial | Original | - | 2025-01 |
| logo_cadena100 | Original | - | 2025-01 |
| logo_kissfm | Original | - | 2025-01 |
| logo_maxima | Original | - | 2025-01 |
| logo_rac1 | Original | - | 2025-01 |
| logo_rne | Original | - | 2025-01 |

## Mexico

| Logo | Source | URL | Date |
|------|--------|-----|------|
| logo_los40_mx | Original | - | 2025-01 |
| logo_exa_fm | Original | - | 2025-01 |
| logo_la_mejor | Original | - | 2025-01 |
| logo_ke_buena | Original | - | 2025-01 |
| logo_joya | iHeart Radio | https://i.iheart.com/v3/re/new_assets/5ef0ca447ec97a064f3322cb | 2026-01-06 |
| logo_amor_fm | Wikimedia Commons | https://commons.wikimedia.org/wiki/File:XHSH-FM.png | 2026-01-06 |
| logo_universal | Universal 88.1 Official (SVG) | https://editorial.universal881.com/wp-content/uploads/2023/04/Logo_Universal_23.svg | 2026-01-07 |
| logo_reactor | Original | - | 2025-01 |
| logo_mvs_noticias | Wikimedia Commons | https://commons.wikimedia.org/wiki/File:Noticias_MVS_102.5.svg | 2026-01-06 |
| logo_w_radio | Original | - | 2025-01 |
| logo_sabrosita | Grupo Radiorama (GCS) | https://storage.googleapis.com/nrm-web/sabrosita/LOGO_SABROSITA_NEW.png | 2026-01-06 |
| logo_alfa_913 | Zeno.fm | https://proxy.zeno.fm/content/stations/5766fc5b-ce3e-4f46-a5b3-c274ea46d8e6/image/ | 2026-01-06 |

## International

| Logo | Source | URL | Date |
|------|--------|-----|------|
| logo_bbc_radio1 | Original | - | 2025-01 |
| logo_capital_fm | Original | - | 2025-01 |
| logo_classic_fm | Original | - | 2025-01 |

---

## Logos Needing Replacement

These logos need to be replaced with proper transparent versions:

- [ ] `logo_bbc_radio1` - Corrupted/too small
- [ ] `logo_capital_fm` - Washed out
- [ ] `logo_classic_fm` - Damaged
