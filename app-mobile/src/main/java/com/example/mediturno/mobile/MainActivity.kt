package com.example.mediturno.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.mediturno.shared.data.DatabaseUrlStore
import com.example.mediturno.shared.data.FirebaseRestRepository
import com.example.mediturno.shared.model.Turno
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

private val Indigo = Color(0xFF4F46E5)
private val IndigoDark = Color(0xFF312E81)
private val Soft = Color(0xFFF5F5FF)
private val Success = Color(0xFF0F9D75)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = MediTurnoViewModel.factory(this)

        setContent {
            val viewModel: MediTurnoViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)

            MediTurnoMobileApp(viewModel)
        }
    }
}

data class MobileUiState(
    val turnos: List<Turno> = emptyList(),
    val turnoActual: Turno? = null,
    val cargando: Boolean = false,
    val mensaje: String = "",
    val databaseUrl: String = ""
)

class MediTurnoViewModel(
    private val store: DatabaseUrlStore,
    private val repository: FirebaseRestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MobileUiState(databaseUrl = store.getUrl())
    )
    val uiState: StateFlow<MobileUiState> = _uiState.asStateFlow()

    init {
        if (store.getUrl().isNotBlank()) {
            refrescar()
        }
    }

    fun refrescar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, mensaje = "")
            try {
                val turnos = repository.getTurnos()
                val actual = repository.getTurnoActual()
                _uiState.value = _uiState.value.copy(
                    turnos = turnos,
                    turnoActual = actual,
                    cargando = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    cargando = false,
                    mensaje = e.message ?: "No se pudo cargar la información."
                )
            }
        }
    }

    fun crearTurno(
        paciente: String,
        consultorio: String,
        tipoConsulta: String,
        onCreated: () -> Unit
    ) {
        if (paciente.isBlank() || consultorio.isBlank()) {
            _uiState.value = _uiState.value.copy(
                mensaje = "Captura el nombre del paciente y el consultorio."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, mensaje = "")
            try {
                val actuales = repository.getTurnos()
                val max = actuales.mapNotNull {
                    it.numero.substringAfter("A-", "").toIntOrNull()
                }.maxOrNull() ?: 0

                val numero = "A-${(max + 1).toString().padStart(3, '0')}"
                val turno = Turno(
                    id = UUID.randomUUID().toString(),
                    numero = numero,
                    paciente = paciente.trim(),
                    consultorio = consultorio.trim(),
                    tipoConsulta = tipoConsulta.trim().ifBlank { "Consulta general" },
                    estado = "PENDIENTE",
                    creadoEn = System.currentTimeMillis()
                )

                repository.guardarTurno(turno)
                _uiState.value = _uiState.value.copy(
                    cargando = false,
                    mensaje = "Turno $numero generado correctamente."
                )
                refrescar()
                onCreated()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    cargando = false,
                    mensaje = e.message ?: "No se pudo crear el turno."
                )
            }
        }
    }

    fun llamarSiguiente() {
        val siguiente = _uiState.value.turnos
            .filter { it.estado != "LLAMADO" }
            .minByOrNull { it.creadoEn }

        if (siguiente == null) {
            _uiState.value = _uiState.value.copy(
                mensaje = "No hay turnos pendientes."
            )
            return
        }

        llamarTurno(siguiente)
    }

    fun llamarTurno(turno: Turno) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, mensaje = "")
            try {
                repository.llamarTurno(turno)
                _uiState.value = _uiState.value.copy(
                    cargando = false,
                    mensaje = "${turno.numero} enviado a Smart TV."
                )
                refrescar()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    cargando = false,
                    mensaje = e.message ?: "No se pudo llamar el turno."
                )
            }
        }
    }

    fun guardarUrl(url: String) {
        store.saveUrl(url)
        _uiState.value = _uiState.value.copy(
            databaseUrl = store.getUrl(),
            mensaje = "URL guardada."
        )
        refrescar()
    }

    fun probarConexion() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, mensaje = "")
            val ok = try {
                repository.probarConexion()
            } catch (_: Exception) {
                false
            }
            _uiState.value = _uiState.value.copy(
                cargando = false,
                mensaje = if (ok) "Conexión correcta con Firebase." else "No se pudo conectar."
            )
        }
    }

    companion object {
        fun factory(activity: ComponentActivity): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val store = DatabaseUrlStore(activity.applicationContext)
                    return MediTurnoViewModel(
                        store = store,
                        repository = FirebaseRestRepository(store)
                    ) as T
                }
            }
    }
}

enum class MobileTab { INICIO, NUEVO, CONFIGURACION }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediTurnoMobileApp(viewModel: MediTurnoViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(MobileTab.INICIO) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MediTurno", fontWeight = FontWeight.Bold)
                        Text("Gestión de turnos", fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IndigoDark,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == MobileTab.INICIO,
                    onClick = { tab = MobileTab.INICIO },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Inicio") }
                )
                NavigationBarItem(
                    selected = tab == MobileTab.NUEVO,
                    onClick = { tab = MobileTab.NUEVO },
                    icon = { Icon(Icons.Default.Add, null) },
                    label = { Text("Nuevo") }
                )
                NavigationBarItem(
                    selected = tab == MobileTab.CONFIGURACION,
                    onClick = { tab = MobileTab.CONFIGURACION },
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("Config.") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Soft)
        ) {
            when (tab) {
                MobileTab.INICIO -> InicioScreen(
                    state = state,
                    onRefresh = viewModel::refrescar,
                    onCallNext = viewModel::llamarSiguiente,
                    onCallTurn = viewModel::llamarTurno
                )
                MobileTab.NUEVO -> NuevoTurnoScreen(
                    state = state,
                    onCreate = { paciente, consultorio, consulta ->
                        viewModel.crearTurno(paciente, consultorio, consulta) {
                            tab = MobileTab.INICIO
                        }
                    }
                )
                MobileTab.CONFIGURACION -> ConfiguracionScreen(
                    state = state,
                    onSave = viewModel::guardarUrl,
                    onTest = viewModel::probarConexion
                )
            }

            if (state.cargando) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun InicioScreen(
    state: MobileUiState,
    onRefresh: () -> Unit,
    onCallNext: () -> Unit,
    onCallTurn: (Turno) -> Unit
) {
    val pendientes = state.turnos.filter { it.estado != "LLAMADO" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Turno actual", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, null)
                    Text(" Actualizar")
                }
            }
        }

        item {
            CurrentTurnCard(state.turnoActual)
        }

        item {
            Button(
                onClick = onCallNext,
                enabled = state.databaseUrl.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("LLAMAR SIGUIENTE TURNO")
            }
        }

        if (state.mensaje.isNotBlank()) {
            item {
                MessageCard(state.mensaje)
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text(
                "Turnos pendientes (${pendientes.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (state.databaseUrl.isBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))
                ) {
                    Text(
                        "Configura la URL de Firebase en la pestaña Config. para activar la sincronización.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else if (pendientes.isEmpty()) {
            item {
                Text(
                    "No hay turnos pendientes. Registra uno desde la pestaña Nuevo.",
                    color = Color.DarkGray
                )
            }
        } else {
            items(pendientes, key = { it.id }) { turno ->
                TurnoRow(turno = turno, onCall = { onCallTurn(turno) })
            }
        }
    }
}

@Composable
private fun CurrentTurnCard(turno: Turno?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = IndigoDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("TURNO ACTUAL", color = Color.White.copy(alpha = 0.8f))
            Text(
                turno?.numero ?: "---",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                turno?.paciente ?: "Sin turno llamado",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            if (turno != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    turno.consultorio,
                    color = Color.White,
                    modifier = Modifier
                        .background(Indigo, RoundedCornerShape(50))
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TurnoRow(turno: Turno, onCall: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(Soft, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    turno.numero,
                    fontWeight = FontWeight.Bold,
                    color = Indigo
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(turno.paciente, fontWeight = FontWeight.SemiBold)
                Text(
                    "${turno.consultorio} • ${turno.tipoConsulta}",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
            }

            OutlinedButton(onClick = onCall) {
                Text("Llamar")
            }
        }
    }
}

@Composable
private fun NuevoTurnoScreen(
    state: MobileUiState,
    onCreate: (String, String, String) -> Unit
) {
    var paciente by remember { mutableStateOf("") }
    var consultorio by remember { mutableStateOf("Consultorio 1") }
    var consulta by remember { mutableStateOf("Consulta general") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Nuevo turno", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Registra al paciente y genera un número de atención.",
            color = Color.DarkGray
        )

        OutlinedTextField(
            value = paciente,
            onValueChange = { paciente = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre del paciente") },
            singleLine = true
        )

        OutlinedTextField(
            value = consultorio,
            onValueChange = { consultorio = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Consultorio") },
            singleLine = true
        )

        OutlinedTextField(
            value = consulta,
            onValueChange = { consulta = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tipo de consulta") },
            singleLine = true
        )

        Button(
            onClick = { onCreate(paciente, consultorio, consulta) },
            enabled = state.databaseUrl.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("GENERAR TURNO")
        }

        if (state.databaseUrl.isBlank()) {
            Text(
                "Primero configura la URL de Firebase.",
                color = Color(0xFFB45309)
            )
        }

        if (state.mensaje.isNotBlank()) {
            MessageCard(state.mensaje)
        }
    }
}

@Composable
private fun ConfiguracionScreen(
    state: MobileUiState,
    onSave: (String) -> Unit,
    onTest: () -> Unit
) {
    var url by remember(state.databaseUrl) { mutableStateOf(state.databaseUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Configuración", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Usa la misma URL de Realtime Database en Mobile y Smart TV.",
            color = Color.DarkGray
        )

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("URL de Firebase") },
            placeholder = { Text("https://proyecto-default-rtdb.firebaseio.com") }
        )

        Button(
            onClick = { onSave(url) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("GUARDAR CONFIGURACIÓN")
        }

        OutlinedButton(
            onClick = onTest,
            enabled = state.databaseUrl.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("PROBAR CONEXIÓN")
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Para la demostración", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("1. Crea Realtime Database en Firebase.")
                Text("2. Copia la URL de la base.")
                Text("3. Guárdala aquí y también en la aplicación TV.")
                Text("4. Registra un turno y pulsa “Llamar siguiente”.")
            }
        }

        if (state.mensaje.isNotBlank()) {
            MessageCard(state.mensaje)
        }
    }
}

@Composable
private fun MessageCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (
                message.contains("correct", ignoreCase = true) ||
                message.contains("enviado", ignoreCase = true) ||
                message.contains("guardada", ignoreCase = true)
            ) Color(0xFFD1FAE5) else Color.White
        )
    ) {
        Text(
            message,
            modifier = Modifier.padding(14.dp),
            color = if (message.contains("correct", true)) Success else Color.DarkGray
        )
    }
}
