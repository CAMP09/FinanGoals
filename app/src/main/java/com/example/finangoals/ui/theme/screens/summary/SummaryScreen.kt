package com.example.finangoals.ui.theme.screens.summary


import android.graphics.Color
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.finangoals.db.FirestoreManager
import com.example.finangoals.ui.theme.navigation.NavRoutes
import com.example.finangoals.ui.theme.theme.Green
import com.example.finangoals.ui.theme.theme.Orange
import com.example.finangoals.ui.theme.theme.White
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

//Función que recibe por parametro un objeto navController para la navegación entre pantallas ,
// y los datos recogidos del usuario de los campos ingresos y gastos para realizar un gráfico pastel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Summary(navController: NavController, firestoreManager: FirestoreManager, ingresos: Float, gastos: Float) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Estado Financiero") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        //Contenido del Scaffold
        content = { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(White)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    //Llamamos a la función que ejecutará el gráfico la cual recibe como parámetros los ingresos y los gastos
                    PieChartGrafic(ingresos, gastos)
                    Text(
                        "¿Listo para empezar a planificar y alcanzar tus objetivos? ¡Vamos allá!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    //Al hacer click en comenzar navega a la pantalla principal de la app
                    Button(
                        onClick = { firestoreManager.addInitialTransactions(
                            ingresos.toDouble(),
                            gastos.toDouble(),
                            onSuccess = {
                                // Navega a la pantalla principal tras el éxito
                                Log.d("AddInitialTransactions", "Transacciones iniciales añadidas correctamente")
                                navController.navigate(NavRoutes.Home.route)
                            },
                            onFailure = { errorMsg ->
                                // Muestra un mensaje de error si algo sale mal
                                Log.e("AddInitialTransactions", "Error al añadir transacciones iniciales: $errorMsg")
                            }
                        )},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Comenzar")
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    )
}

//Función de diseño y configuración del gráfico tipo pastel, recibe como parámetros los ingresos y gastos
@Composable
fun PieChartGrafic(ingresos: Float, gastos: Float) {
    val rentaDisponible = ingresos - gastos
    /*
    AndroidView nos permite integrar PieChart del repositorio de mikelphil en github (java) en un composable de Jetpack
     */
    AndroidView(
        modifier = Modifier.size(300.dp),
        factory = { context ->
            PieChart(context).apply {//Inicializamos el PieChart

                //Creamos una variable de tipo grafic que nos permitirá añadir los elementos que necesitamos para el gráfico
                val grafic  = mutableListOf<PieEntry>()
                grafic .apply {
                add(PieEntry(ingresos, "Ingresos"))
                add(PieEntry(gastos, "Gastos"))
                add(PieEntry(rentaDisponible, "Ahorros"))
            }

                //Definimos los colores
                val colors = intArrayOf(
                    Orange.toArgb(),
                    Color.LTGRAY,
                    Green.toArgb()
                )

                //Configuramos el gráfico
                val dataSet = PieDataSet(grafic , "").apply {
                    setSliceSpace(3f)
                    selectionShift = 5f
                    setColors(colors, 255)
                    valueTextSize = 12f
                    valueTextColor = Color.BLACK
                }
                this.data = PieData(dataSet)
                this.description.isEnabled = false
                this.centerText = ""
                this.setUsePercentValues(true)
                this.isDrawHoleEnabled = true
                this.setEntryLabelColor(Color.BLACK)
                this.setEntryLabelTextSize(12f)
                this.animateY(1200)
                this.legend.isEnabled = false
            }
        },
        update = { pieChart ->
            pieChart.notifyDataSetChanged()
            pieChart.invalidate()
        }
    )
}


