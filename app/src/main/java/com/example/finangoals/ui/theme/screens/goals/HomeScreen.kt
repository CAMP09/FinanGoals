package com.example.finangoals.ui.theme.screens.goals

import android.content.Context
import android.util.Log
import android.view.ContextThemeWrapper
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.finangoals.R
import com.example.finangoals.db.FirestoreManager
import com.example.finangoals.db.FirestoreManagerData
import com.example.finangoals.db.TransactionData
import com.example.finangoals.db.TransactionType
import com.example.finangoals.ui.theme.navigation.BottomNavigationBar
import com.example.finangoals.ui.theme.navigation.NavRoutes
import com.example.finangoals.ui.theme.navigation.TopBar
import com.example.finangoals.ui.theme.screens.login.AuthController
import com.example.finangoals.ui.theme.theme.LightGray
import com.example.finangoals.ui.theme.theme.Orange
import com.example.finangoals.ui.theme.theme.White
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, authController: AuthController, firestoreManager: FirestoreManager) {
    var showDialog by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var goalsList by remember { mutableStateOf(listOf<FirestoreManagerData>()) }
    var showDP by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val context = LocalContext.current

    // Función DatePickerDialog que muestra un dialogo de tipo DatePicker para que el usuario seleccione una fecha
    fun datePickerDialog() {
        val themeContext = ContextThemeWrapper(context, R.style.CustomDatePickerDialog)
        android.app.DatePickerDialog(
            themeContext,
            //Función lambda que se ejecuta cuando el usuario selecciona una fecha
            { _: DatePicker, year, monthOfYear, dayOfMonth ->

                //Creamos una variable con la fecha selecionada por el usuario, fecha date picker, sumamos 1 para convertir
                // a LocalDate y la formateamos
                val selectedDate = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
                if (selectedDate.isBefore(LocalDate.now())) {
                    Toast.makeText(
                        context,
                        "La fecha seleccionada no puede ser anterior a la fecha actual",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    endDate = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                }
            },
            //Fecha inicial Date Picker que se muestra por defecto cuando el usuario lo abre, los meses en Date Picker empiezan en 0
            //por eso quitamos 1 a monthValue, mientras que en LocaDate los meses empiezan en 1.
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        ).show()
    }

    // Muestra el Date Picker cuando showDP es true se llama a la función datePickerDialog() y después cambia su estado a false
    LaunchedEffect(showDP) {
        if (showDP) {
            datePickerDialog()
            showDP = false
        }
    }

    // Carga inicial de los datos de las metas del usuario que haya en Home, si la carga es exitosa actualiza
    // el contenido y lo muestra en caso contrario lanza un mensaje de error
    LaunchedEffect(key1 = true) {
        firestoreManager.listAllGoals(
            onSuccess = { loadedGoals ->
                goalsList = loadedGoals
            },
            onFailure = { errorMsg ->
                Toast.makeText(context, "La carga ha fallado $errorMsg", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialogo que se muestra cuando el usuario añade una meta nueva a la lista
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Añadir un objetivo nuevo") },
            text = {
                Column {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Importe") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)

                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { },
                        label = { Text("Fecha fin") },
                        readOnly = true,
                        //Definimos un parámetro de tipo trilingIcon que nos permitirá situar un icono
                        //con funcionalidad al final del campo, es este caso el calendario
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDP = true },
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
            },
            // Funcionalidad de los botones de confirmar y cancelar del dialogo, el botón de confirmar
            // confirmará que los datos introducidos son correctos, esto es que cumplen con las reglas de validación que
            // hemos definido en las funciones
            confirmButton = {
                Button(
                    onClick = {
                        // Verificar el contenido de los campos
                        if (validateDescription(description, context) && validateAmount(amountText,context) && validateDate(endDate, context)
                        ) {
                            val amount = amountText.toDouble()
                            val newGoal = FirestoreManagerData(
                                description = description,
                                amount = amount,
                                endDate = endDate
                            )
                            // Verificar si la fecha es válida, si no lo es no se podrá añadir la meta
                            if (LocalDate.parse(endDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                    .isBefore(LocalDate.now())
                            ) {
                                Toast.makeText(
                                    context,
                                    "La fecha seleccionada no puede ser anterior a la fecha actual",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                firestoreManager.addGoal(newGoal,
                                    onSuccess = { goalId ->
                                        goalsList = goalsList + newGoal.copy(id = goalId)
                                        Toast.makeText(
                                            context,
                                            "Objetivo añadido correctamente",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        // Se cierra el dialogo y se resetean los valores
                                        showDialog = false
                                        description = ""
                                        amountText = ""
                                        endDate = LocalDate.now()
                                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                )
                            }
                        }
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    //Diseño y estructura del layout. Incluye un top bar y una barra baja de navegación entre las distintas pantallas de la app.
    Scaffold(
        topBar = { TopBar(navController, authController) },
        bottomBar = { BottomNavigationBar(navController = navController, currentRoute = NavRoutes.Home.route) },
        //Diseño y funcionalidad del botón flotante. Al hacer click en el muestra el dialogo para que el usuario introduzca los
        //datos de la meta a añadir
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showDialog = true
                    description = ""
                    amountText = ""
                    endDate = ""
                },
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .size(72.dp),
                containerColor = Orange
            ) {
                Icon(Icons.Filled.Add,
                    contentDescription = "Añadir objetivo",
                    tint = White,
                    modifier = Modifier.size(30.dp))
            }
        },
        floatingActionButtonPosition = FabPosition.Center
        //El Scaffold contiene un innerPadding,con el que evitamos que se sobrepongan elementos de la UI.
        //Contiene un lazzycolum para mostrtar una lista de elementos/metas introducidos por el usuario,
        //además contará con la funcionalidad de los botones edit y delete, de manera que el usuario podrá
        //tanto editar una meta existente como eliminarla.
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            items(goalsList) { goal ->
                GoalItem(
                    goal = goal,
                    navController = navController,
                    delete = { goalId, isComplete ->
                        if (isComplete) {
                            val accomplishedGoal = TransactionData(description = goal.description, amount = goal.amount, month =LocalDate.now().monthValue -1, type = TransactionType.EXPENSE, userId = goal.userId)
                            firestoreManager.addTransaction(accomplishedGoal, onSuccess = { Log.i("Meta alcanzada",
                                "Transacción agregada") },
                                onFailure = { Log.i("Meta alcanzada","La transacción no se ha podido agregar") })
                            Toast.makeText(context, "Meta alcanzada", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Meta eliminada", Toast.LENGTH_SHORT).show()
                        }
                        firestoreManager.delete(goalId, onSuccess = {
                            goalsList = goalsList.filterNot { it.id == goalId }
                        }, onFailure = {
                            Toast.makeText(context, "Error al eliminar meta", Toast.LENGTH_SHORT)
                                .show()
                        })
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.height(130.dp)) // Espacio adicional al final de la lista
            }
        }
    }
}

@Composable
fun GoalItem(goal: FirestoreManagerData, navController: NavHostController, delete: (String, Boolean) -> Unit) {

    // Creamos la variable progress que guardará el grado de avance de la meta, cuánto ha avanzado y
    // cuánto queda para llegar a su fecha fin.
    val progress = calculateProgress(goal.endDate, goal.amount, goal.aport)

    //Definimos las variables relacionadas con los iconos de la card
    val isComplete = progress == 1.0f
    val check: ImageVector
    val checkContentDescrip: String
    val deleteIcon: ImageVector = ImageVector.vectorResource(id = R.drawable.delete)
    val deleteContentDescrip: String = "Eliminar"

    //Si la meta se ha acanzado se cambia el icono 'delete' por 'check' y la descripcion sino se mantiene el icono 'delete'
    if (isComplete) {
        check = Icons.Filled.Check
        checkContentDescrip = "Completado"
    } else {
        check = deleteIcon
        checkContentDescrip = deleteContentDescrip
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
            Column (
                modifier = Modifier
                    .fillMaxWidth(0.7f)
            ) {
                Text(buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                        append("Objetivo: ")
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(goal.description)
                    }
                })
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Importe: ${goal.amount} €")
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Fecha fin: ${goal.endDate}")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    delete(goal.id, isComplete)
                }) {
                    Icon(
                        imageVector = check,
                        contentDescription = checkContentDescrip,
                        modifier = Modifier.size( 28.dp)
                    )
                }
            }
        }
        if (progress != null) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(6.dp)
                    .padding(horizontal = 4.dp),
            )
        }
    }
}

/*
Función que se encarga de calcular el avance de la barra de progreso hasta la fecha fin.
Recibe una fecha como cadena de texto y nos devuelve un valor float entre 0-1 (0%-100%).
Creamos un objeto de tipo DateTimeFormatter, que se encargará de formatear cualquier tipo de fecha que nos
llegue con el patrón especificado.
La variable endDate recibe la fecha fin introducida por el usuario y la formatea con df, devolviéndonos un
objeto de tipo LocalDate.
Y la variable todayDate guardará la fecha actual calculada por el método now()
*/

fun calculateProgress(endDateGoal: String, amount: Double, aportMonth: Double): Float? {
    val df = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val endDate = LocalDate.parse(endDateGoal, df)
    val todayDate = LocalDate.now() // fecha actual
    // Definimos el inicio del periodo con LocalDate.of
    val start = LocalDate.of(todayDate.year, 1, 1)

    // Si la fecha objetivo es anterior al inicio del año, devolvemos null (no mostrar barra de progreso)
    if (amount == 0.0 || aportMonth == 0.0) {
        return 0f
    }
    // Si las aportaciones totales son iguales o mayores al importe del objetivo, progreso al 100%
    if (aportMonth >= amount) {
        return 1.0f
    }

    // Si la fecha actual es igual o posterior a la fecha de finalización, progreso al 100%
    if (todayDate.isEqual(endDate) || todayDate.isAfter(endDate)) {
        return 1.0f
    }

    // Calculamos el total de días en el periodo, es decir, el total de días que hay desde hoy hasta la fecha fin
    // introducida por el usuario y lo guardamos en la variable totalDays.
    // y por otro lado calculamos los días que han pasado desde el inicio hasta hoy para saber el % de avance de la barra
    // de progreso y así poder calcular lo que falta hasta llegar a la fecha fin.
    val totalDays = ChronoUnit.DAYS.between(start, endDate).toInt()
    val daysPassed = ChronoUnit.DAYS.between(start, todayDate).toInt()
    val timeProgress = daysPassed.toFloat() / totalDays.toFloat()

    // Calcular progreso basado en la cantidad aportada
    val amountProgress = if (amount > 0) aportMonth.toFloat() / amount.toFloat() else 0f

    // Combinar ambos progresos
    return (timeProgress + amountProgress) / 2
}

// Funciones de validación de las cajas de texto a través de expresiones regulares usando la función toRegex()
fun validateDescription(textBox: String, context: Context): Boolean {
    if (textBox.isEmpty()) {
        Toast.makeText(context, "Campo no puede ser vacío", Toast.LENGTH_SHORT).show()
        return false
    }
    // Con el método toRegex() convertimos una cadena de texto en una expresión regular, admite
    // en este caso la caja de texto admite solo letras
    if (!textBox.matches("^[a-zA-Z\\s]+$".toRegex())) {
        Toast.makeText(context, "La descripción solo debe contener letras", Toast.LENGTH_SHORT).show()
        return false
    }
    return true
}

// Función de validación del campo importe
fun validateAmount(amountBox: String, context: Context): Boolean {
    if (amountBox.isEmpty()) {
        Toast.makeText(context, "Campo no puede estar vacío", Toast.LENGTH_SHORT).show()
        return false
    }
    if (!amountBox.matches("^[0-9]*\\.?[0-9]+$".toRegex())) {
        Toast.makeText(context, "El importe debe ser numérico", Toast.LENGTH_SHORT).show()
        return false
    }
    return true
}

// Función de validación del campo fecha
fun validateDate(date: String, context: Context): Boolean {
    if (date.isEmpty()) {
        Toast.makeText(context, "Campo no puede estar vacío", Toast.LENGTH_SHORT).show()
        return false
    }
    // Verificación formato fecha (intentar hacerlo con un calendario)
    if (!date.matches("^\\d{2}/\\d{2}/\\d{4}$".toRegex())) {
        Toast.makeText(context, "Formato de fecha incorrecto. DD/MM/YYYY", Toast.LENGTH_SHORT).show()
        return false
    }
    return true
}

@Preview(showBackground = true)
@Composable
fun HomeShow() {
    val navController = rememberNavController()
    HomeScreen(navController, AuthController(LocalContext.current), FirestoreManager(LocalContext.current))
}
