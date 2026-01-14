# Política de Privacidad - RadioFlow+

**Fecha de entrada en vigor:** 15 de enero de 2026  
**Última actualización:** 15 de enero de 2026

## 1. Introducción

Bienvenido a RadioFlow+ ("nosotros", "nuestro" o "la aplicación"). Esta Política de Privacidad explica cómo recopilamos, utilizamos, divulgamos y protegemos tu información cuando usas nuestra aplicación móvil.

Al usar RadioFlow+, aceptas la recopilación y el uso de información de acuerdo con esta política.

## 2. Información que Recopilamos

### 2.1 Datos Personales
RadioFlow+ está diseñada con la privacidad en mente. **No recopilamos, almacenamos ni transmitimos** ninguna información de identificación personal (PII) a servidores externos.

### 2.2 Datos Almacenados Localmente en tu Dispositivo
Los siguientes datos se almacenan **únicamente de forma local en tu dispositivo**:
- **Preferencias de Usuario:** Configuración de tema, modo de vista seleccionado (lista/cuadrícula), preferencias del temporizador de sueño
- **Emisoras Favoritas:** Emisoras de radio que marcas como favoritas
- **Configuración de Alarmas:** Horas de alarma configuradas y emisoras seleccionadas
- **Estado de Reproducción:** Última emisora reproducida para la funcionalidad de reanudación automática
- **Estado Premium:** Indicador local del estado de suscripción premium (verificado mediante Google Play Billing)

### 2.3 Permisos que Solicitamos
RadioFlow+ solicita los siguientes permisos para proporcionar la funcionalidad principal:

- **INTERNET:** Necesario para transmitir emisoras de radio
- **POST_NOTIFICATIONS (Android 13+):** Para mostrar controles de reproducción en el área de notificaciones
- **FOREGROUND_SERVICE:** Para mantener la reproducción activa en segundo plano
- **SCHEDULE_EXACT_ALARM (Android 12+):** Permiso crítico ("Funcionalidad Principal") para garantizar que la radio suene a la hora exacta configurada por el usuario. Sin este permiso, la función de despertador no sería fiable.
- **USE_EXACT_ALARM (Android 14+):** Necesario para la función principal de Radio Alarma en Android 14+.
- **WAKE_LOCK:** Para evitar que el dispositivo se suspenda durante la reproducción
- **RECEIVE_BOOT_COMPLETED:** Para restaurar alarmas después del reinicio del dispositivo

**Importante:** Solo solicitamos permisos esenciales para la funcionalidad principal de radio. No solicitamos permisos para contactos, cámara, micrófono o SMS.

## 3. Cómo Utilizamos tu Información

Todos los datos se procesan **localmente en tu dispositivo** para los siguientes propósitos:
- Transmitir las emisoras de radio que seleccionas
- Recordar tus emisoras favoritas
- Mantener tus preferencias de tema e interfaz
- Activar alarmas de radio en los horarios programados
- Reanudar automáticamente tu última emisora cuando vuelvas a abrir la app
- Gestionar tu estado de suscripción premium

## 4. Compartición de Datos y Terceros

### 4.1 No Vendemos Datos
**No vendemos, comercializamos ni alquilamos** tu información personal a terceros.

### 4.2 Servicios de Google Play
RadioFlow+ utiliza la Biblioteca de Facturación de Google Play para procesar suscripciones premium. Google puede recopilar datos de transacciones según su política de privacidad: https://policies.google.com/privacy

### 4.3 Proveedores de Transmisión de Radio
Cuando reproduces una emisora de radio, tu dispositivo se conecta directamente al servidor de transmisión de la emisora. No controlamos ni tenemos visibilidad sobre qué datos pueden recopilar estos servidores de terceros (típicamente dirección IP y metadatos de conexión).

### 4.4 Bibliotecas de Terceros
RadioFlow+ utiliza las siguientes bibliotecas de código abierto:
- **Bibliotecas AndroidX** (Google) - Componentes de UI y sistema
- **ExoPlayer (Media3)** - Transmisión de audio
- **Jetpack Compose** - Framework de UI moderna
- **Glance** - Framework de widgets de pantalla de inicio

Estas bibliotecas pueden procesar datos localmente en tu dispositivo pero no transmiten tu información personal a servidores externos.

## 5. Privacidad de Menores

RadioFlow+ **no está dirigida a menores de 16 años**. No recopilamos intencionalmente información personal de niños. Si eres padre o tutor y crees que tu hijo nos ha proporcionado información personal, por favor contáctanos.

## 6. Seguridad de los Datos

Implementamos medidas de seguridad estándar de la industria:
- Todas las preferencias se almacenan utilizando SharedPreferences cifradas de Android
- No se transmiten datos sensibles a través de la red
- La validación de suscripciones premium se gestiona de forma segura mediante Google Play

## 7. Tus Derechos (Cumplimiento RGPD)

Si te encuentras en la Unión Europea o Reino Unido, tienes los siguientes derechos:
- **Derecho de Acceso:** Solicitar una copia de los datos almacenados sobre ti (todos los datos están en tu dispositivo)
- **Derecho de Rectificación:** Corregir cualquier dato inexacto (mediante la configuración de la app)
- **Derecho de Supresión:** Eliminar todos los datos de la app desinstalándola o borrando los datos de la app en la configuración del dispositivo
- **Derecho a la Portabilidad de Datos:** Exportar tus favoritos y configuraciones (función disponible en la configuración de la app)
- **Derecho de Oposición:** Puedes dejar de usar la app en cualquier momento

## 8. Retención de Datos

- **Preferencias de Usuario:** Conservadas hasta que desinstalas la app o borras los datos de la app
- **Estado de Reproducción:** Conservado durante 48 horas para la funcionalidad de reanudación automática
- **Configuraciones de Alarma:** Conservadas hasta que las deshabilitas o eliminas
- **Suscripción Premium:** Validada con Google Play en cada inicio de la app; no se almacenan datos históricos

## 9. Cambios a esta Política de Privacidad

Podemos actualizar esta Política de Privacidad ocasionalmente. Te notificaremos de cualquier cambio mediante:
- Actualización de la fecha "Última actualización" al principio de esta política
- Mostrar una notificación en la app en el primer inicio después de la actualización

## 10. Transferencias Internacionales de Datos

Dado que todos los datos se almacenan localmente en tu dispositivo, no hay transferencias internacionales de datos. La única conexión externa es a los servidores de transmisión de radio que eliges reproducir.

## 11. Contacto

Si tienes alguna pregunta sobre esta Política de Privacidad, por favor contáctanos en:

**Correo electrónico:** jonatanpradasn@gmail.com  
**Nombre de la App:** RadioFlow+  
**Desarrollador:** Jonatan Pradas Navarro

## 12. Base Legal para el Procesamiento (RGPD)

Nuestra base legal para procesar tus datos:
- **Consentimiento:** Proporcionas tu consentimiento al usar la app y aceptar esta política
- **Interés Legítimo:** Proporcionar funcionalidad de transmisión de radio y mejorar la experiencia del usuario
- **Contrato:** Cumplir con nuestro acuerdo de suscripción con usuarios premium
