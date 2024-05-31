package com.example.finangoals.ui.theme.screens.goals

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.finangoals.R
import com.example.finangoals.db.FirestoreManager
import com.example.finangoals.db.FirestoreManagerData
import com.example.finangoals.ui.theme.navigation.NavRoutes
import com.example.finangoals.ui.theme.theme.Orange
import com.example.finangoals.ui.theme.theme.White
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun GoalScreen(navController: NavHostController, goalId: String) {
    val context = LocalContext.current
    val fm = FirestoreManager(context)
    var goal by remember { mutableStateOf<FirestoreManagerData?>(null) }
    val isEditing = remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDP by remember { mutableStateOf(false) }
    var endDate by remember { mutableStateOf("") }


    // Carga el objetivo cuando el Composable es cargado o cuando el goalId cambia
    LaunchedEffect(goalId) {
        fm.getGoal(goalId,
            onSuccess = { loadedGoal ->
                goal = loadedGoal
            },
            onFailure = {
                Toast.makeText(context, "Error al cargar los detalles del objetivo", Toast.LENGTH_SHORT).show()
                navController.popBackStack()  // Regresa a la pantalla anterior si falla la carga
            }
        )

    }

    fun datePickerDialog() {
        val themeContext = ContextThemeWrapper(context, R.style.CustomDatePickerDialog)
        android.app.DatePickerDialog(
            themeContext,
            //Función lambda que se ejecuta cuando el usuario selecciona una fecha
            { _: DatePicker, year, monthOfYear, dayOfMonth ->

                //Creamos una variable con la fecha selecionada por el usuario (fecha date picker) y la formateamos
                val selectedDate = LocalDate.of(
                    year,
                    monthOfYear + 1,
                    dayOfMonth
                )//sumamos 1 para convertir a LocalDate
                endDate = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            },

            //Fecha inicial Date Picker que se muestra por defecto cuando el usuario lo abre, los meses en Date Picker empiezan en 0
            //por eso quitamos 1 a monthValue, mientras que en LocaDate los meses empiezan en 1.
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        ).show()
    }
//Muestra el Date Picker cuando showDP es true se llama a la función datePickerDialog() y después cambia su estado a false
    LaunchedEffect(showDP) {
        if (showDP) {
            datePickerDialog()
            showDP = false
        }
    }

    Scaffold(
        topBar = { GoalTopBar(navController) },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(
                        horizontal = 16.dp,
                        vertical = 16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                goal?.let { g ->
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!isEditing.value) {
                            DetailItem(label = "Descripción", value = g.description)
                            DetailItem(label = "Cantidad", value = "${g.amount} €")
                            DetailItem(label = "Aportaciones", value = "${g.aport} €")
                            DetailItem(label = "Restante", value = "${g.amount - g.aport}")
                            DetailItem(label = "Aportación en %", value = calculatePercentage(g))
                            DetailItem(label = "Fecha de Fin", value = g.endDate)
                        } else {
                            OutlinedTextField(
                                value = g.description,
                                onValueChange = { newDesc -> goal = g.copy(description = newDesc) },
                                label = { Text("Descripción") }
                            )
                            OutlinedTextField(
                                value = g.amount.toString(),
                                onValueChange = { newAmount ->
                                    newAmount.toDoubleOrNull()?.let {
                                        goal = g.copy(amount = it)
                                    }
                                },
                                label = { Text("Cantidad") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = endDate,
                                onValueChange = { },
                                label = { Text("Fecha fin") },
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable() {
                                        showDP = true
                                    },
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Progress(goal!!)
                    }
                }
            }
        },
        floatingActionButton = { EditSaveFAB(isEditing, goal, goalId, fm, context) },
        floatingActionButtonPosition = FabPosition.Center
        )
}

fun calculatePercentage(goal: FirestoreManagerData): String {
    val percentage = if (goal.amount != 0.0) {
        (goal.aport / goal.amount) * 100
    } else {
        0.0
    }
    return "%.2f".format(percentage) + "%"
}


//Función con el topbar de la app, funcionalidad del botón de cierre.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalTopBar(navController: NavHostController) {
    CenterAlignedTopAppBar(
        title = { Text(text = "Objetivo") },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Orange,
            titleContentColor = White
        ),
        navigationIcon = {
            IconButton(onClick = {
                navController.popBackStack() }) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Volver", tint = White)
            }
        }
    )
}

// Función para el FAB de edición y guardado
@Composable
fun EditSaveFAB(isEditing: MutableState<Boolean>, goal: FirestoreManagerData?, goalId: String, fm: FirestoreManager, context: Context) {
    FloatingActionButton(onClick = {
        if (!isEditing.value) {
            // Entering edit mode
            isEditing.value = true
        } else {
            // Saving changes
            if (goal != null) {
                fm.update(goalId, goal,
                    onSuccess = {
                        Toast.makeText(context, "Objetivo actualizado con éxito", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = {
                        Toast.makeText(context, "Error al actualizar el objetivo", Toast.LENGTH_SHORT).show()
                    }
                )
                isEditing.value = false
            }
        }
    },
        shape = CircleShape,
        modifier = Modifier.padding(bottom = 24.dp).size(72.dp),
        containerColor = Orange
    ) {
        Icon(
            imageVector = if (isEditing.value) Icons.Filled.Check else Icons.Filled.Edit,
            contentDescription = if (isEditing.value) "Guardar" else "Editar",
            tint = White,
            modifier = Modifier.size(25.dp)
        )
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = label, color = Color.Gray, fontSize = 24.sp)
        Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Progress(goal: FirestoreManagerData){
    val progress = calculateProgress(goal.endDate, goal.amount, goal.aport)
    LinearProgressIndicator(
        progress =  progress ?: 0f,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(24.dp)
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(16.dp)),
    )
}