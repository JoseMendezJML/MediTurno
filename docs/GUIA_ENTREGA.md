# Guía rápida para la entrega

## Nombre sugerido de la solución
**MediTurno – Sistema inteligente de gestión y visualización de turnos médicos**

## Descripción breve
MediTurno es un sistema compuesto por una aplicación móvil y una aplicación para Smart TV desarrolladas con Jetpack Compose. El personal de una clínica registra y llama turnos desde el teléfono, mientras la Smart TV consulta la información sincronizada y muestra automáticamente el número del turno, paciente y consultorio correspondiente.

## Capturas recomendadas
1. App Mobile – Inicio y turno actual.
2. App Mobile – Registro de nuevo turno.
3. App Mobile – Lista de turnos pendientes.
4. App TV – Turno actual.
5. App TV – Historial de últimos turnos.
6. Firebase – estructura `mediturno/`.

## Flujo para el video
1. Mostrar Smart TV esperando turno.
2. Registrar paciente en Mobile.
3. Pulsar `Llamar siguiente`.
4. Mostrar cómo Smart TV cambia automáticamente.
5. Repetir con un segundo turno para demostrar el historial.

## Configuración adicional
La solución utiliza Firebase Realtime Database como servicio externo. Ambas aplicaciones deben configurarse con la misma URL de Realtime Database. La aplicación TV consulta la base aproximadamente cada 1.5 segundos, por lo que los cambios hechos desde Mobile se reflejan casi en tiempo real.
