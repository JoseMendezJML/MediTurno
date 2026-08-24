package com.example.mediturno.shared.data

import com.example.mediturno.shared.model.Turno
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class FirebaseRestRepository(
    private val store: DatabaseUrlStore
) {
    suspend fun getTurnos(): List<Turno> = withContext(Dispatchers.IO) {
        val response = request("GET", "/mediturno/turnos.json")
        if (response.isBlank() || response == "null") return@withContext emptyList()

        val root = JSONObject(response)
        val result = mutableListOf<Turno>()
        val keys = root.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            val item = root.optJSONObject(key) ?: continue
            result += item.toTurno(key)
        }

        result.sortedBy { it.creadoEn }
    }

    suspend fun getTurnoActual(): Turno? = withContext(Dispatchers.IO) {
        val response = request("GET", "/mediturno/turnoActual.json")
        if (response.isBlank() || response == "null") return@withContext null
        JSONObject(response).toTurno()
    }

    suspend fun guardarTurno(turno: Turno) = withContext(Dispatchers.IO) {
        request(
            method = "PUT",
            path = "/mediturno/turnos/${turno.id}.json",
            body = turno.toJson().toString()
        )
    }

    suspend fun llamarTurno(turno: Turno) = withContext(Dispatchers.IO) {
        val llamado = turno.copy(estado = "LLAMADO")

        request(
            method = "PUT",
            path = "/mediturno/turnos/${llamado.id}.json",
            body = llamado.toJson().toString()
        )

        request(
            method = "PUT",
            path = "/mediturno/turnoActual.json",
            body = llamado.toJson().toString()
        )
    }

    suspend fun probarConexion(): Boolean = withContext(Dispatchers.IO) {
        try {
            request("GET", "/mediturno.json")
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun request(
        method: String,
        path: String,
        body: String? = null
    ): String {
        val base = store.getUrl()
        require(base.isNotBlank()) {
            "Configura primero la URL de Firebase Realtime Database."
        }

        val connection = (URL("$base$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 7000
            readTimeout = 7000
            doInput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            if (body != null) {
                doOutput = true
                outputStream.use { stream ->
                    stream.write(body.toByteArray(Charsets.UTF_8))
                }
            }
        }

        val code = connection.responseCode
        val source = if (code in 200..299) connection.inputStream else connection.errorStream

        val text = source?.use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                buildString {
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        append(line)
                    }
                }
            }
        }.orEmpty()

        connection.disconnect()

        if (code !in 200..299) {
            error("Firebase respondió HTTP $code: $text")
        }

        return text
    }

    private fun Turno.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("numero", numero)
        put("paciente", paciente)
        put("consultorio", consultorio)
        put("tipoConsulta", tipoConsulta)
        put("estado", estado)
        put("creadoEn", creadoEn)
    }

    private fun JSONObject.toTurno(fallbackId: String = "") = Turno(
        id = optString("id", fallbackId),
        numero = optString("numero", ""),
        paciente = optString("paciente", ""),
        consultorio = optString("consultorio", ""),
        tipoConsulta = optString("tipoConsulta", ""),
        estado = optString("estado", "PENDIENTE"),
        creadoEn = optLong("creadoEn", 0L)
    )
}
