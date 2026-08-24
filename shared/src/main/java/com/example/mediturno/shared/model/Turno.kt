package com.example.mediturno.shared.model

data class Turno(
    val id: String = "",
    val numero: String = "",
    val paciente: String = "",
    val consultorio: String = "",
    val tipoConsulta: String = "",
    val estado: String = "PENDIENTE",
    val creadoEn: Long = 0L
)
