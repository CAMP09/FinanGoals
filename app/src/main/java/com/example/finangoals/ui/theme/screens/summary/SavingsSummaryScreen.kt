package com.example.finangoals.ui.theme.screens.summary

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.example.finangoals.db.FirestoreManager
import com.example.finangoals.db.FirestoreManagerData
import com.example.finangoals.ui.theme.navigation.BottomNavigationBar
import com.example.finangoals.ui.theme.navigation.NavRoutes
import com.example.finangoals.ui.theme.navigation.TopBar
import com.example.finangoals.ui.theme.screens.goals.calculateProgress
import com.example.finangoals.ui.theme.screens.login.AuthController
import com.example.finangoals.ui.theme.theme.Green
import com.example.finangoals.ui.theme.theme.LightGray
import com.example.finangoals.ui.theme.theme.White
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlin.math.max
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsSummaryScreen(navController: NavHostController, firestoreManager: FirestoreManager, authController: AuthController) {
    // Creamos una variable que tendrá la lista de tareas
    var goalsList by remember { mutableStateOf(listOf<FirestoreManagerData>()) }
    var savings by remember { mutableStateOf(0.0) }
    val context = LocalContext.current
    val totalAport = remember { mutableStateOf(0.0) }
    val updatedSavings = rememberSaveable { mutableStateOf(savings) }

    // Carga de los datos desde Firebase
    LaunchedEffect(Unit) {
        firestoreManager.listAllGoals(
            onSuccess = {loadedGoals ->
                goalsList = loadedGoals
                Log.i("Metas", "Metas cargadas")
            },
            onFailure = { error ->
                Toast.makeText(context, "Error al listar objetivos: $error", Toast.LENGTH_SHORT).show()
            }
        )
        firestoreManager.calculateSavings(
            onSuccess = { newSavings ->
                savings = newSavings
            },
            onFailure = { error ->
                Toast.makeText(context, "Error al calcular ahorros: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }
    LaunchedEffect(goalsList) {
        totalAport.value = goalsList.sumOf { it.aport }
    }

    LaunchedEffect(savings, totalAport.value) {
        updatedSavings.value = max(0.0, savings - totalAport.value)
    }

    // Diseño y composición del top bar
    Scaffold(
        topBar = { TopBar(navController = navController, authController = authController) },
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = NavRoutes.SavingsSummary.route)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(White)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Función que mostrará el gráfico en el caso de que haya objetivos pendientes de completar
            // mostrará el importe en aportaciones acumulado y descripción del objetivo
            if (goalsList.isNotEmpty()) {
                PieChartView(context = context, aportaciones = goalsList.map { it.amount.toFloat() }, descriptions = goalsList.map { it.description })
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tus ahorros son de: ${updatedSavings.value} €",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = Green
                )
            }
            // Scroll
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Mostramos la lista de objetivos y el detalle de los mismos
                items(goalsList) { goal ->
                    GoalSummaryItem(goal = goal, totalAport = totalAport, updatedSavings = updatedSavings, firestoreManager = firestoreManager, navController = navController)
                }
            }
        }
    }
}

// Función que se encarga de representar un gráfico tipo pastel con las aportaciones y descripción de las metas
@Composable
fun PieChartView(context: Context, aportaciones: List<Float>, descriptions: List<String>) {
    AndroidView(factory = { context ->
        PieChart(context).apply {
            val entries = mutableListOf<PieEntry>()
            for (index in aportaciones.indices) {
                val aportacion = aportaciones[index]
                val description = descriptions[index]
                val entry = PieEntry(aportacion, description)
                entries.add(entry)
            }

            val dataSet = PieDataSet(entries, "Aportaciones").apply {
                colors = entries.map {
                    Color.rgb(
                        Random.nextInt(100, 256),
                        Random.nextInt(100, 256),
                        Random.nextInt(100, 256)
                    )
                }
                valueTextColor = Color.WHITE
                valueTextSize = 14f
            }
            this.data = PieData(dataSet)
            this.description = Description().apply {
                text = ""
            }
            this.legend.isEnabled = false
            this.animateY(1200)
        }
    },
        modifier = Modifier
            .fillMaxWidth()
            .height(225.dp))
}

// Función que se encargará de mostrar una tarjeta con información detallada de las metas
@Composable
fun GoalSummaryItem(goal: FirestoreManagerData, totalAport: MutableState<Double>, updatedSavings: MutableState<Double>, firestoreManager: FirestoreManager, navController: NavHostController) {
    var showDialog by remember { mutableStateOf(false) }
    var newAportacion by remember { mutableStateOf("") }
    val context = LocalContext.current


    // Calcular el restante como la diferencia entre el importe del objetivo y la suma de las aportaciones
    val restante = goal.amount - goal.aport

    //revisar %
    val aportacionPercent = if (goal.amount != 0.0) {
        (goal.aport / goal.amount) * 100
    } else {
        0.0
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nueva aportación") },
            text = {
                Column {
                    Text(text = "Introduce la nueva aportación para el objetivo: ${goal.description}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newAportacion,
                        onValueChange = { newAportacion = it },
                        label = { Text("Aportación") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val aportacionValue = newAportacion.toDoubleOrNull()
                        if (aportacionValue != null && aportacionValue <= updatedSavings.value) {
                            val totalNewAportacion = goal.aport + aportacionValue
                            totalAport.value += aportacionValue
                            firestoreManager.updateGoalAport(goal.id, totalNewAportacion, {
                                goal.aport = totalNewAportacion
                                showDialog = false
                                newAportacion = ""
                            }, { error ->
                                Toast.makeText(context, "Error al actualizar aportación: $error", Toast.LENGTH_SHORT).show()
                            })
                        } else {
                            Toast.makeText(context, "La aportación no puede ser mayor a tus ahorros actuales o inválida", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Añadir")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable {
                navController.navigate("goal/${goal.id}")
            },
        colors = CardDefaults.cardColors(containerColor = LightGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                        append("Objetivo: ")
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(goal.description)
                    }
                })
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Importe: ${goal.amount} €", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Aportaciones: ${String.format("%.2f", goal.aport)} €", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Restante: $restante €", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Aportación en %: ${"%.2f".format(aportacionPercent)}%", fontSize = 18.sp)
            }
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add,
                    contentDescription = "Editar",
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}