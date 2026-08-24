# MediTurno

Proyecto académico para la actividad **“Ejercicio Final entre mobile y Smart TV”**.

## Módulos

- `app-mobile`: aplicación Android para registrar y llamar turnos.
- `app-tv`: aplicación Android TV que muestra el turno actual.
- `shared`: modelos, acceso REST a Firebase Realtime Database y preferencias compartidas por lógica.

## Tecnologías

- Kotlin
- Jetpack Compose
- Android / Android TV
- Coroutines
- Firebase Realtime Database mediante REST

## Abrir el proyecto

1. Descomprime `MediTurno.zip`.
2. Abre Android Studio.
3. Selecciona **Open** y elige la carpeta `MediTurno`.
4. Espera la sincronización de Gradle.
5. Ejecuta `app-mobile` en teléfono/emulador Android.
6. Ejecuta `app-tv` en un emulador Android TV o dispositivo compatible.

> Si Android Studio solicita actualizar Gradle/AGP, puedes aceptar la actualización recomendada.

## Configuración de Firebase

El proyecto compila sin `google-services.json`. Para sincronizar Mobile ↔ TV se usa la API REST de Realtime Database.

1. En Firebase Console crea un proyecto.
2. Agrega **Realtime Database**.
3. Para la demostración académica, inicia la base en modo de prueba.
4. Copia la URL, por ejemplo:
   `https://tu-proyecto-default-rtdb.firebaseio.com`
5. Abre **Configuración** en MediTurno Mobile y guarda esa URL.
6. Abre **Configurar conexión** en MediTurno TV y guarda la misma URL.

### Reglas solo para demostración

Archivo incluido: `firebase_rules_demo.json`.


## Flujo de prueba

1. En Mobile abre `Nuevo`.
2. Registra un paciente.
3. Regresa a `Inicio`.
4. Pulsa `Llamar siguiente`.
5. La aplicación TV actualizará el turno automáticamente.

## Estructura de datos

```text
mediturno/
├── turnoActual/
└── turnos/
    ├── <id-turno>/
    └── ...
```

## Git sugerido

```bash
git init
git add .
git commit -m "chore: crear proyecto base MediTurno"
```

