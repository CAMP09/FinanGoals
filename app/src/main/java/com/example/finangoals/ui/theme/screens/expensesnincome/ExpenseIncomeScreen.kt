package com.example.finangoals.ui.theme.screens.expensesnincome


import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.finangoals.R
import com.example.finangoals.db.FirestoreManager
import com.example.finangoals.db.TransactionData
import com.example.finangoals.db.TransactionType
import com.example.finangoals.ui.theme.navigation.BottomNavigationBar
import com.example.finangoals.ui.theme.navigation.NavRoutes
import com.example.finangoals.ui.theme.navigation.TopBar
import com.example.finangoals.ui.theme.screens.login.AuthController
import com.example.finangoals.ui.theme.theme.Orange
import com.example.finangoals.ui.theme.theme.OrangeTransparent
import com.example.finangoals.ui.theme.theme.White
import com.example.finangoals.ui.theme.theme.Green
import com.example.finangoals.ui.theme.theme.LightGray
import com.github.tehras.charts.piechart.PieChart
import com.github.tehras.charts.piechart.PieChartData
import com.github.tehras.charts.piechart.animation.simpleChartAnimation
import com.github.tehras.charts.piechart.renderer.SimpleSliceDrawer
import java.time.LocalDate


@Composable
fun ExpenseIncomeScreen(navController: NavHostController, authController: AuthController, firestoreManager: FirestoreManager) {
    var showDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var typeTransaction by remember { mutableStateOf(TransactionType.EXPENSE) }
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var transactionList by remember { mutableStateOf(listOf<TransactionData>()) }
    var editTransactionId by remember { mutableStateOf("") }
    val savings = remember { mutableDoubleStateOf(0.0) }

    var period by remember { mutableStateOf("Mes") }
    var filteredTransactions by remember { mutableStateOf(listOf<TransactionData>()) }

    val months = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
    val currentMonth = LocalDate.now().monthValue - 1
    var monthIndex by remember { mutableIntStateOf(currentMonth) }
    var monthText by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Carga inicial de los datos de las transacciones del usuario, si la carga es exitosa actualiza
    //el contenido y lo muestra en caso contrario lanza un mensaje de error
    LaunchedEffect(key1 = true) {
        firestoreManager.listTransactions(
            onSuccess = { loadedTransactions ->
                transactionList = loadedTransactions
                filteredTransactions = filterTransactions(loadedTransactions, period)
                firestoreManager.calculateSavings(
                    onSuccess = { newSavings ->
                        savings.doubleValue = newSavings
                    },
                    onFailure = { error ->
                        Toast.makeText(context, "Error al calcular ahorros: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onFailure = { errorMsg ->
                Toast.makeText(context, "La carga ha fallado $errorMsg", Toast.LENGTH_SHORT).show()
            }
        )
    }
    // Actualiza las transacciones filtradas cuando el período cambia
    LaunchedEffect(period, transactionList) {
        filteredTransactions = filterTransactions(transactionList, period)
    }

    //Dialogo que se muestra cuando el usuario añade una transacción nueva a la lista
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Añadir movimiento") },
            text = {
                Column {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción") }
                    )
                    Row {
                        RadioButton(
                            selected = typeTransaction == TransactionType.EXPENSE,
                            onClick = { typeTransaction = TransactionType.EXPENSE }
                        )
                        Text("Gasto")
                        RadioButton(
                            selected = typeTransaction == TransactionType.INCOME,
                            onClick = { typeTransaction = TransactionType.INCOME }
                        )
                        Text("Ingreso")
                    }
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Importe") }
                    )
                    MonthSelector(currentMonthIndex = monthIndex, onMonthChange = { newMonth ->
                        monthIndex = months.indexOf(newMonth)})
                }
            },
            //Funcionalidad de los botones de confirmar y cancelar del dialogo, el botón de confirmar
            //confirmará que los datos introducidos son correctos, esto es que cumplen con las reglas de validación que
            //hemos definido en las funciones
            confirmButton = {
                Button(
                    onClick = {
                        if (validateDescription(description, context) && validateAmount(amountText, context)) {
                            //Constante que nos serevirá para asegurarnos de que el String que nos llega lo podemos convertir en un número válido.
                            //En caso de que falle devuelve null.Si la conversión es correcta se crea una nueva transacción con los parámetros indicados
                            //y se añade a la lista de transacciones del ususario tanto en la app como en firebase
                            val amount = amountText.toDoubleOrNull()
                            if (amount != null) {
                                val newTransaction = TransactionData(description = description, amount = amount, type = typeTransaction, month = monthIndex)
                                firestoreManager.addTransaction(newTransaction,
                                    onSuccess = { transactionId ->
                                        transactionList = transactionList + newTransaction.copy(id = transactionId)//está configurado para que la bese de datos asigne un id
                                        Toast.makeText(context, "Movimiento añadido correctamente", Toast.LENGTH_SHORT).show()
                                        //se cierra el dialogo y se resetean los valores
                                        showDialog = false
                                        description = ""
                                        amountText = ""
                                        monthText = ""
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()

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
            }
        )
    }

    //Diálogo que se muestra cuando el usuario modifica datos de un movimiento existente
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar movimiento") },
            text = {
                Column {
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción del movimiento") }
                    )
                    Row {
                        RadioButton(
                            selected = typeTransaction == TransactionType.EXPENSE,
                            onClick = { typeTransaction = TransactionType.EXPENSE }
                        )
                        Text("Gasto")
                        RadioButton(
                            selected = typeTransaction == TransactionType.INCOME,
                            onClick = { typeTransaction = TransactionType.INCOME }
                        )
                        Text("Ingreso")
                    }
                    TextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Importe") }
                    )
                    MonthSelector(currentMonthIndex = monthIndex, onMonthChange = { newMonth ->
                        monthIndex = months.indexOf(newMonth)})
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (validateDescription(description, context) && validateAmount(amountText, context)) {
                            val amount = amountText.toDoubleOrNull()
                            if (amount != null) {
                                val updatedTransaction = TransactionData(description = description, amount = amount, type = typeTransaction, month = monthIndex)
                                firestoreManager.updateTransaction(editTransactionId, updatedTransaction,
                                    onSuccess = {
                                        transactionList = transactionList.map { if (it.id == editTransactionId) updatedTransaction.copy(id = editTransactionId) else it }
                                        Toast.makeText(context, "Tus movimientos se han  actualizado correctamente", Toast.LENGTH_SHORT).show()
                                        showEditDialog = false
                                        description = ""
                                        amountText = ""
                                        monthText = ""
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(context, "Error al actualizar tu movimiento: $error", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                ) {
                    Text("Actualizar")
                }
            },
            dismissButton = {
                Button(onClick = { showEditDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    //Diseño y estructura del layout. Incluye un top bar y una barra baja de navegación entre las distintas pantallas de la app.
    Scaffold(
        topBar = { TopBar(navController, authController) },
        bottomBar = { BottomNavigationBar(navController = navController, currentRoute = NavRoutes.ExpenseIncome.route) },
        //Diseño y funcionalidad del botón flotante. Al hacer click en el muestra el dialogo para que el usuario introduzca los
        //datos del movimiento a añadir
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showDialog = true
                    description = ""
                    amountText = ""
                    monthText = ""
                },
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .size(72.dp),
                containerColor = Orange
            ) {
                Icon(Icons.Filled.Add,
                    contentDescription = "Añadir movimiento",
                    tint = White,
                    modifier = Modifier.size(30.dp))
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    )
    //El Scaffold contiene un innerPadding,con el que evitamos que se sobrepongan elementos de la UI.
    //Contiene un lazzycolum para mostrtar una lista de elementos/movimientos introducidos por el usuario,
    //además contará con la funcionalidad de los botones edit y delete, de manera que el usuario podrá
    //tanto editar un movimiento existente como eliminarla.
    { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PieChartView(filteredTransactions)
                    Spacer(modifier = Modifier.height(16.dp))
                    TotalsView(filteredTransactions)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            TimePeriodSelector { selectedPeriod ->
                period = selectedPeriod
                filteredTransactions = filterTransactions(transactionList, period)
            }
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                items(filteredTransactions) { transactionData ->
                    TransactionItem(
                        transaction = transactionData,
                        edit = { transactionId ->
                            firestoreManager.getTransaction(transactionId, onSuccess = { selectedTransaction ->
                                description = selectedTransaction.description
                                amountText = selectedTransaction.amount.toString()
                                monthText = months[selectedTransaction.month]
                                editTransactionId = transactionId
                                showDialog = false
                                showEditDialog = true
                            }, onFailure = {
                                Toast.makeText(context, "Error al obtener el movimiento", Toast.LENGTH_SHORT).show()
                            })
                        },

                        delete = { transactionId ->
                            firestoreManager.deleteTransaction(transactionId, onSuccess = {
                                transactionList = transactionList.filterNot { it.id == transactionId }
                                filteredTransactions = filterTransactions(transactionList, period)
                                Toast.makeText(context, "Movimiento eliminado", Toast.LENGTH_SHORT).show()
                            }, onFailure = {
                                Toast.makeText(context, "Error al eliminar movimiento", Toast.LENGTH_SHORT).show()
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
}

@Composable
fun TransactionItem(transaction: TransactionData, edit: (String) -> Unit, delete: (String) -> Unit) {
    val months = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
    val transactionTypeText = if (transaction.type == TransactionType.EXPENSE) "Gasto" else "Ingreso"

    val cardColor = if (transaction.type == TransactionType.EXPENSE) LightGray else OrangeTransparent
    val textColor = Color.Black

    /*
    Diseñamos una Card en dónde se guardarán los movimientos introducidas por el usuario. La Card estará formada
    por los datos introducidos por el usuario, el boton edit y delete y en la parte baja de la card se mostrará
    una barra de progreso que indicará el grado de avanace de los movimientos.
    */
    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
            ) {
                Text(buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                        append(transactionTypeText + ": ")
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(transaction.description)
                    }
                })
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Importe: ${transaction.amount} €", color = textColor)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Mes: ${months[transaction.month]}", color = textColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
            ){
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { edit(transaction.id) }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.pencil),
                        contentDescription = "Editar",
                        modifier = Modifier.size(25.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { delete(transaction.id) }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.delete),
                        contentDescription = "Eliminar",
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
        }
    }
}

/*
Función que se encarga de calcular el avance de la barra de progreso hasta la fecha fin.
Recibe una fecha como cadena de texto y nos delvuelve un valor float enrte 0-1 (0%-100%).
Creamos un objeto de tipo DateTimeFormatter, que se encargará de formatear cualquier tipo de fehca que nos
llegue con el patrón especificado.
La variable endDate recibe la fecha fin introducida por el usuario y la formatea con df, devolviéndonos un
objeto de tipo LocalDate.
Y la variable todayDate guardará la fecha actual calculada por el método now()
 */

//Función que filtra las transacciones según el período seleccionado
fun filterTransactions(transactions: List<TransactionData>, period: String): List<TransactionData> {
    val currentDate = LocalDate.now()
    return when (period) {
        "Mes" -> transactions.filter { it.month == currentDate.monthValue - 1 }
        "Trimestre" -> transactions.filter {
            val currentMonth = currentDate.monthValue
            val startMonth = (currentMonth - 3 + 12) % 12
            val endMonth = currentMonth
            it.month in startMonth until endMonth || it.month == endMonth
        }
        "Año" -> transactions.filter {
            val startOfYear = currentDate.withDayOfYear(1)
            val endOfYear = currentDate.withDayOfYear(currentDate.lengthOfYear())
            val transactionDate = LocalDate.of(currentDate.year, it.month + 1, 1)
            !transactionDate.isBefore(startOfYear) && !transactionDate.isAfter(endOfYear)
        }
        else -> transactions
    }
}

//Funciones de validación  de las cajas de texto a través de expresiones regulares usando la función toRegex()
fun validateDescription(textBox: String, context: Context): Boolean {
    if (textBox.isEmpty()) {
        Toast.makeText(context, "Campo no puede ser vacío", Toast.LENGTH_SHORT).show()
        return false
    }
    //Con el método toRegex() convertimos una cadena de texto en una expresión regular, admite
    // en este caso la caja de texto admite solo letras
    if (!textBox.matches("^[a-zA-Z\\s]+$".toRegex())) {
        Toast.makeText(context, "La descripción solo debe contener letras", Toast.LENGTH_SHORT)
            .show()
        return false
    }
    return true
}
//Función de validación del campo importe
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

@Composable
fun MonthSelector(currentMonthIndex: Int, onMonthChange: (String) -> Unit) {
    val months = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
    var monthIndex by remember { mutableStateOf(currentMonthIndex) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            monthIndex = if (monthIndex > 0) monthIndex - 1 else 11
            onMonthChange(months[monthIndex])
        }) {
            Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Mes anterior")
        }
        Text(months[monthIndex],
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center)
        IconButton(onClick = {
            monthIndex = if (monthIndex < 11) monthIndex + 1 else 0
            onMonthChange(months[monthIndex])
        }) {
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Mes siguiente")
        }
    }
}

@Composable
fun PieChartView(transactions: List<TransactionData>) {
    val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val filteredSavings = income - expense

    val pieChartData = PieChartData(
        slices = listOf(
            PieChartData.Slice(value = income.toFloat(), color = Orange),
            PieChartData.Slice(value = expense.toFloat(), color = LightGray),
            PieChartData.Slice(value = filteredSavings.toFloat(), color = Green)
        )
    )

    PieChart(
        pieChartData = pieChartData,
        modifier = Modifier
            .fillMaxWidth(0.5f)
            .fillMaxHeight(0.2f),
        animation = simpleChartAnimation(),
        sliceDrawer = SimpleSliceDrawer()
    )
}
@Composable
fun TotalsView(transactions: List<TransactionData>) {
    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val filteredSavings = totalIncome - totalExpense

    Spacer(modifier = Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Text(
            text = "Ingresos: $totalIncome €",
            fontWeight = FontWeight.Bold,
            color = Orange
        )
        Text(
            text = "Gastos: $totalExpense €",
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Ahorros: $filteredSavings €",
            fontWeight = FontWeight.Bold,
            color = Green
        )
    }
}
@Composable
fun TimePeriodSelector(onPeriodChange: (String) -> Unit) {
    val orangeColor = Color(0xFFF47C00)
    val orangeTransparent = Color(0x33F47C00)
    val transparent = Color(0x00000000)

    var selectedPeriod by remember { mutableStateOf("Mes") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // Define un botón para cada período
        val periods = listOf("Mes", "Trimestre", "Año")
        periods.forEach { period ->
            Button(
                onClick = {
                    onPeriodChange(period)
                    selectedPeriod = period
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (period == selectedPeriod) orangeTransparent else transparent,
                    contentColor = orangeColor
                )
            ) {
                Text(period, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ExpenseIncomeShow() {
    val navController = rememberNavController()
    ExpenseIncomeScreen(navController, AuthController(LocalContext.current), FirestoreManager(LocalContext.current))
}

