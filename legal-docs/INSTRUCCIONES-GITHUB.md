# Instrucciones para Subir Documentos Legales a GitHub

## 📋 Resumen
He creado:
1. ✅ Política de Privacidad (GDPR compliant)
2. ✅ Términos del Servicio
3. ✅ Licencias de Terceros
4. ✅ Sección Legal en la app (Settings → Legal e Información)

## 🚀 Pasos para Subir a GitHub

### 1. Crear Repositorio en GitHub (vía web)

1. Ve a https://github.com/new
2. **Repository name:** `radioflowplus`
3. **Description:** `Sitio web oficial y documentación legal de RadioFlow+ - La mejor app de radio para Android`
4. Marca como **Public** (obligatorio para GitHub Pages gratis)
5. Click **Create repository**

> 💡 **Tip SEO:** Descripción en español porque tu audiencia es hispanohablante. Google rankea mejor contenido en el idioma del usuario.

### 2. Subir Documentos (ejecuta estos comandos)

```powershell
# Navegar a la carpeta legal-docs
cd "C:\Users\jonat\Documents\antigravity\RadioAndroid\legal-docs"

# Inicializar repositorio Git
git init

# Añadir todos los archivos
git add .

# Crear commit inicial
git commit -m "Add legal documentation for RadioFlow+"

# Conectar con tu repositorio (REEMPLAZA tu-usuario con tu username de GitHub)
git remote add origin https://github.com/TU-USUARIO/radioflowplus.git

# Subir a GitHub
git branch -M main
git push -u origin main
```

### 3. Activar GitHub Pages

1. Ve a tu repositorio: `https://github.com/TU-USUARIO/radioflowplus`
2. Click en **Settings** (⚙️)
3. En el menú izquierdo, click **Pages**
4. En "Source", selecciona **main** branch
5. Click **Save**
6. Espera 1-2 minutos

### 4. Verificar que funciona

Tus URLs serán:
- **Privacy Policy:** `https://TU-USUARIO.github.io/radioflowplus/privacy-policy`
- **Terms of Service:** `https://TU-USUARIO.github.io/radioflowplus/terms-of-service`
- **Third-Party Licenses:** `https://TU-USUARIO.github.io/radioflowplus/third-party-licenses`

💡 **Ventaja:** En el futuro puedes añadir landing page en `https://TU-USUARIO.github.io/radioflowplus/` para marketing!

### 5. Actualizar la App con tus URLs

**IMPORTANTE:** Antes de compilar, edita estos archivos:

#### A. `LegalSheet.kt` (líneas 54, 66, 78)
```kotlin
// CAMBIA ESTO:
val url = "https://YOUR-USERNAME.github.io/radioflowplus/privacy-policy"

// POR ESTO (con tu usuario):
val url = "https://TU-USUARIO.github.io/radioflowplus/privacy-policy"
```

Haz lo mismo para las 3 URLs (privacy-policy, terms-of-service, third-party-licenses)

#### B. `LegalSheet.kt` (línea 123)
```kotlin
// CAMBIA ESTO:
"📧 support@radioflowapp.com"

// POR:
"📧 TU-EMAIL@ejemplo.com"
```

#### C. Documentos .md (si quieres personalizarlos)

En `privacy-policy.md` y `terms-of-service.md`, busca y reemplaza:
- `[YOUR_SUPPORT_EMAIL@example.com]` → Tu email real
- `[YOUR_DEVELOPER_NAME]` → Tu nombre o nombre de empresa
- `[YOUR_JURISDICTION]` → Tu jurisdicción (ej: "España", "Madrid, España")

## 📱 Google Play Console

Una vez que GitHub Pages esté activo:

1. Ve a [Google Play Console](https://play.google.com/console)
2. Selecciona tu app
3. **App content** → **Privacy policy**
4. Añade: `https://TU-USUARIO.github.io/radioflowplus/privacy-policy`
5. Save

✨ **Bonus SEO:** Comparte el link del repo en redes, foros, etc. para mejorar indexación.

## 🔄 Actualizar Documentos en el Futuro

```powershell
cd "C:\Users\jonat\Documents\antigravity\RadioAndroid\legal-docs"

# Edita los archivos .md que necesites

# Subir cambios
git add .
git commit -m "Update privacy policy"
git push
```

Cambios estarán live en 1-2 minutos.

## ✅ Checklist Final

- [ ] Repositorio creado en GitHub
- [ ] Documentos subidos con git push
- [ ] GitHub Pages activado
- [ ] URLs verificadas (funcionan)
- [ ] LegalSheet.kt actualizado con URLs reales
- [ ] Email de soporte actualizado
- [ ] Placeholders reemplazados en .md files
- [ ] App compilada y probada
- [ ] URL añadida a Google Play Console

## 🆘 Problemas Comunes

**"fatal: not a git repository"**
→ Asegúrate de estar en la carpeta `legal-docs`

**"Permission denied (publickey)"**
→ Primera vez usando GitHub desde esta PC, usa HTTPS en vez de SSH
→ Git te pedirá usuario/password de GitHub

**"GitHub Pages no carga"**
→ Espera 5 minutos, a veces tarda
→ Verifica que el repo sea Public

**URLs dan 404**
→ Quita `.md` de la URL (GitHub Pages lo hace automático)
→ Ejemplo: usa `/privacy-policy` NO `/privacy-policy.md`

---

**¿Necesitas ayuda?** Avísame cuando estés en cada paso y te ayudo.
