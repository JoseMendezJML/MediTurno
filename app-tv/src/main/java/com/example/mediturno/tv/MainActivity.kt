package com.example.mediturno.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val Navy = Color(0xFF11114A)
private val Purple = Color(0xFF4F46E5)
private val PurpleSoft = Color(0xFF312E81)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = TvViewModel.factory(this)

        setContent {
            val viewModel: TvViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
            MediTurnoTvApp(viewModel)
        }
    }
}

data class TvUiState(
    val turnoActual: Turno? = null,
    val historial: List<Turno> = emptyList(),
    val databaseUrl: String = "",
    val mensaje: String = ""
)

class TvViewModel(
    private val store: DatabaseUrlStore,
    private val repository: FirebaseRestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TvUiState(databaseUrl = store.getUrl())
    )
    val uiState: StateFlow<TvUiState> = _uiState.asStateFlow()

    init {
        iniciarSincronizacion()
    }

    private fun iniciarSincronizacion() {
        viewModelScope.launch {
            while (isActive) {
                if (store.getUrl().isNotBlank()) {
                    try {
                        val actual = repository.getTurnoActual()
                        val historial = repository.getTurnos()
                            .filter { it.estado == "LLAMADO" }
                            .sortedByDescending { it.creadoEn }
                            .take(5)

                        _uiState.value = _uiState.value.copy(
                            turnoActual = actual,
                            historial = historial,
                            databaseUrl = store.getUrl(),
                            mensaje = ""
                        )
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            mensaje = e.message ?: "Error de conexión."
                        )
                    }
                }
                delay(1500)
            }
        }
    }

    fun guardarUrl(url: String) {
        store.saveUrl(url)
        _uiState.value = _uiState.value.copy(
            databaseUrl = store.getUrl(),
            mensaje = "Configuración guardada."
        )
    }

    companion object {
        fun factory(activity: ComponentActivity): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val store = DatabaseUrlStore(activity.applicationContext)
                    return TvViewModel(
                        store,
                        FirebaseRestRepository(store)
                    ) as T
                }
            }
    }
}

@Composable
fun MediTurnoTvApp(viewModel: TvViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var configuring by remember { mutableStateOf(state.databaseUrl.isBlank()) }

    BackHandler(enabled = configuring && state.databaseUrl.isNotBlank()) {
        configuring = false
    }

    if (configuring) {
        TvConfigurationScreen(
            currentUrl = state.databaseUrl,
            message = state.mensaje,
            onSave = {
                viewModel.guardarUrl(it)
                configuring = false
            }
        )
    } else {
        TvDashboard(
            state = state,
            onConfigure = { configuring = true }
        )
    }
}

@Composable
private fun TvDashboard(
    state: TvUiState,
    onConfigure: () -> Unit
) {
    val current = state.turnoActual

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF09092D), Navy, Color(0xFF16165D))
                )
            )
            .padding(horizontal = 54.dp, vertical = 34.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "MediTurno",
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Sistema de gestión de turnos",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                }

                OutlinedButton(onClick = onConfigure) {
                    Text("Configurar conexión", color = Color.White)
                }
            }

            Spacer(Modifier.height(30.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(26.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier
                        .weight(1.25f)
                        .fillMaxSize(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(34.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "TURNO ACTUAL",
                            color = Purple,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            current?.numero ?: "---",
                            fontSize = 82.sp,
                            fontWeight = FontWeight.Black,
                            color = PurpleSoft
                        )

                        Text(
                            current?.paciente ?: "Esperando llamado",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(14.dp))

                        if (current != null) {
                            Text(
                                current.consultorio.uppercase(),
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Purple, RoundedCornerShape(50))
                                    .padding(horizontal = 28.dp, vertical = 10.dp)
                            )

                            Spacer(Modifier.height(18.dp))

                            Text(
                                "Por favor diríjase al consultorio indicado",
                                color = Color.DarkGray,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(0.75f)
                        .fillMaxSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp)
                    ) {
                        Text(
                            "Últimos turnos",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(18.dp))

                        if (state.historial.isEmpty()) {
                            Text(
                                "Aún no hay turnos llamados.",
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        } else {
                            state.historial.forEach { turno ->
                                HistoryItem(turno)
                                Spacer(Modifier.height(12.dp))
                            }
                        }

                        if (state.mensaje.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                state.mensaje,
                                color = Color(0xFFFFD166),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(turno: Turno) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            turno.numero,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(95.dp)
        )
        Column {
            Text(
                turno.paciente,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                turno.consultorio,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun TvConfigurationScreen(
    currentUrl: String,
    message: String,
    onSave: (String) -> Unit
) {
    var url by remember(currentUrl) { mutableStateOf(currentUrl) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.72f),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(34.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Configurar MediTurno TV",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = PurpleSoft
                )
                Text(
                    "Escribe la misma URL de Firebase Realtime Database que configuraste en la aplicación móvil.",
                    color = Color.DarkGray
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("URL de Firebase") },
                    placeholder = {
                        Text("https://proyecto-default-rtdb.firebaseio.com")
                    },
                    singleLine = true
                )

                Button(
                    onClick = { onSave(url) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("GUARDAR Y MOSTRAR TURNOS")
                }

                if (message.isNotBlank()) {
                    Text(message, color = Color.DarkGray)
                }
            }
        }
    }
}
